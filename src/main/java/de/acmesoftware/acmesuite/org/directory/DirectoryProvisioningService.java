package de.acmesoftware.acmesuite.org.directory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.org.domain.RoleAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Makes the directory mirror the workforce (ADR-0011): the HR-facing orchestration on top of the
 * vendor-neutral {@link DirectoryProvisioner} port. It decides WHO and WHAT; the adapter decides
 * HOW against one protocol.
 *
 * <p>The rules are the ADR's decisions, made concrete:
 * <ul>
 *   <li><b>Members, not applicants</b> (§6). An applicant is a candidate, not staff — giving them
 *       an account grants a sign-in subject to someone who was never hired.</li>
 *   <li><b>Leaving disables, never deletes</b> (§7). An inactive person's account is blocked and
 *       left standing as evidence that they existed.</li>
 *   <li><b>Idempotent over the login name</b> (§4): a run needs no memory of the last one. The
 *       object id is written back only when it changed.</li>
 *   <li><b>Groups from the mapping, empty means untouched</b> (§5).</li>
 *   <li><b>Dry-run first</b> (§8): with {@code dryRun} nothing is written, the plan is logged.</li>
 * </ul>
 */
public class DirectoryProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(DirectoryProvisioningService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PersonRepository persons;
    private final RoleAssignmentRepository roleAssignments;
    private final DirectoryProvisioner directory;
    private final DirectoryProperties props;

    public DirectoryProvisioningService(PersonRepository persons, RoleAssignmentRepository roleAssignments,
                                        DirectoryProvisioner directory, DirectoryProperties props) {
        this.persons = persons;
        this.roleAssignments = roleAssignments;
        this.directory = directory;
        this.props = props;
    }

    @Transactional
    public ProvisioningRun provision() {
        List<Person> all = persons.findAll();
        int skippedApplicants = (int) all.stream().filter(Person::isApplicant).count();
        // Eligible = staff with a login name. Without a login name there is no idempotency key and
        // no address to sign in with; such a record is not yet provisionable.
        List<Person> eligible = all.stream()
                .filter(p -> !p.isApplicant())
                .filter(p -> notBlank(p.getEmail()))
                .toList();

        List<String> errors = new ArrayList<>();
        int created = 0, updated = 0, disabled = 0, passwordsSet = 0;

        for (Person p : eligible) {
            DirectoryPerson dp = toDirectoryPerson(p);
            if (props.dryRun()) {
                log.info("[dry-run] directory upsert login={} mail={} title={} dept={} enabled={}",
                        dp.loginName(), dp.mail(), dp.jobTitle(), dp.department(), dp.active());
                continue;
            }
            try {
                String objectId = p.getDirectoryObjectId();
                if (objectId == null) {
                    objectId = directory.findObjectId(dp.loginName()).orElse(null);
                }
                if (objectId == null) {
                    if (!p.isActive()) {
                        // Someone who left and never had an account: nothing to disable, nothing to
                        // create -- a departed person is not provisioned into existence.
                        continue;
                    }
                    // New account: create carries the password inline (the one shared request).
                    objectId = directory.create(dp, freshPassword());
                    created++;
                    passwordsSet++;
                } else if (!p.isActive()) {
                    // Leaving disables, never deletes (§7): block and leave standing.
                    directory.disable(objectId);
                    disabled++;
                } else {
                    // Existing, active: attributes first, in their own request. The password is a
                    // separate permission and call -- a refusal there does not cost the attributes.
                    directory.updateAttributes(objectId, dp);
                    updated++;
                }
                // Write the anchor back only when it changed (ADR-0011 §4) -- the endpoint that an
                // external run would call is the same store this in-process run writes.
                if (!objectId.equals(p.getDirectoryObjectId())) {
                    p.assignDirectoryObjectId(objectId);
                }
                if (p.isActive()) {
                    syncGroups(p, objectId, errors);
                }
            } catch (RuntimeException e) {
                errors.add(p.getId() + ": " + e.getMessage());
            }
        }

        ProvisioningRun run = new ProvisioningRun(props.dryRun(), eligible.size(),
                created, updated, disabled, passwordsSet, skippedApplicants, errors);
        log.info("directory provisioning done: {}", run);
        return run;
    }

    /**
     * Groups from the configured mapping (§5). Empty mapping -> nothing is touched, so a fresh
     * installation cannot rewrite a customer's group memberships. A person's local role/PoA keys
     * that appear in the mapping are added to the named group.
     */
    private void syncGroups(Person p, String objectId, List<String> errors) {
        if (props.groupMappings().isEmpty()) {
            return;
        }
        for (String localKey : localKeysOf(p)) {
            String groupName = props.groupMappings().get(localKey);
            if (groupName == null) {
                continue;
            }
            try {
                directory.addToGroup(objectId, groupName);
            } catch (RuntimeException e) {
                errors.add(p.getId() + " group " + groupName + ": " + e.getMessage());
            }
        }
    }

    private static DirectoryPerson toDirectoryPerson(Person p) {
        return new DirectoryPerson(
                p.getEmail(), p.fullName(), p.getFirstName(), p.getLastName(),
                p.getEmail(), p.getJobTitle(), department(p), p.getId(), p.isActive());
    }

    private static String department(Person p) {
        return p.getPrimaryOrgUnit() == null ? null : p.getPrimaryOrgUnit().getName();
    }

    /** The local keys of a person that a mapping may reference: role ids today. */
    private List<String> localKeysOf(Person p) {
        // Kept intentionally narrow -- the mapping decides which of these matter; unmapped keys are
        // ignored. Roles are the stable vocabulary the org module already exposes.
        return roleAssignments.findByPerson_Id(p.getId()).stream()
                .map(ra -> ra.getRole().getId()).toList();
    }

    private static String freshPassword() {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        // A directory rejects trivial passwords; a random 24-byte URL-safe string clears every
        // policy and is never used again (the user resets it via the normal flow).
        return "A1!" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
