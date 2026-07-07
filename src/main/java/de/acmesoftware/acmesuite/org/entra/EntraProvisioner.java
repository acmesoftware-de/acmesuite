package de.acmesoftware.acmesuite.org.entra;

import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provisions active persons (not applicants, with an email) from ACMEhr into Microsoft Entra
 * (ADR-0005). Idempotent: the key is the already stored {@code entraObjectId}, otherwise a lookup
 * by the UPN (= email) — only then is a user created. The returned {@code oid} is persisted on the
 * person. Manager relationships are handled in a second pass, once both sides have an {@code oid}.
 *
 * <p>{@code acme.entra.dry-run=true} (default) only plans/logs, without writing to Entra.
 */
@Service
@ConditionalOnProperty(name = "acme.entra.enabled", havingValue = "true")
class EntraProvisioner {

    private static final Logger log = LoggerFactory.getLogger(EntraProvisioner.class);

    private final PersonRepository persons;
    private final EntraGraphClient graph;
    private final EntraProperties props;

    EntraProvisioner(PersonRepository persons, EntraGraphClient graph, EntraProperties props) {
        this.persons = persons;
        this.graph = graph;
        this.props = props;
    }

    @Transactional
    ProvisionSummary provision() {
        List<Person> all = persons.findAll();
        List<Person> eligible = all.stream()
                .filter(p -> !p.isApplicant() && p.getEmail() != null && !p.getEmail().isBlank())
                .toList();
        int skippedApplicants = (int) all.stream().filter(Person::isApplicant).count();

        if (props.dryRun()) {
            for (Person p : eligible) {
                log.info("[dry-run] entra upsert upn={} employeeId={} displayName={} jobTitle={} department={} enabled={}",
                        p.getEmail(), p.getId(), p.fullName(), p.getJobTitle(), department(p), p.isActive());
            }
            log.info("[dry-run] {} Personen wuerden provisioniert, {} Bewerber uebersprungen",
                    eligible.size(), skippedApplicants);
            return new ProvisionSummary(true, eligible.size(), 0, 0, skippedApplicants, 0, List.of());
        }

        int created = 0;
        int updated = 0;
        int managers = 0;
        List<String> errors = new ArrayList<>();
        Map<String, String> oidByPerson = new LinkedHashMap<>();

        for (Person p : eligible) {
            try {
                String oid = p.getEntraObjectId();
                if (oid == null) {
                    oid = graph.findUserId(p.getEmail()).orElse(null);
                }
                if (oid == null) {
                    oid = graph.create(createBody(p));
                    created++;
                } else {
                    graph.update(oid, updateBody(p));
                    updated++;
                }
                p.assignEntraObjectId(oid);
                oidByPerson.put(p.getId(), oid);
            } catch (RuntimeException e) {
                log.warn("entra upsert fehlgeschlagen fuer {}: {}", p.getId(), e.toString());
                errors.add(p.getId() + ": " + e.getMessage());
            }
        }
        persons.saveAll(eligible); // persists the newly set oids

        // Pass 2: manager relationships (only when both sides have an oid).
        for (Person p : eligible) {
            Person mgr = p.getManager();
            if (mgr == null) {
                continue;
            }
            String pOid = oidByPerson.get(p.getId());
            String mOid = oidByPerson.getOrDefault(mgr.getId(), mgr.getEntraObjectId());
            if (pOid != null && mOid != null) {
                try {
                    graph.setManager(pOid, mOid);
                    managers++;
                } catch (RuntimeException e) {
                    errors.add("manager " + p.getId() + ": " + e.getMessage());
                }
            }
        }

        log.info("entra provisioning fertig: created={} updated={} managers={} skippedApplicants={} errors={}",
                created, updated, managers, skippedApplicants, errors.size());
        return new ProvisionSummary(false, eligible.size(), created, updated, skippedApplicants, managers, errors);
    }

    private static String department(Person p) {
        return p.getPrimaryOrgUnit() == null ? null : p.getPrimaryOrgUnit().getName();
    }

    /** Common attributes (settable on update as well). */
    private static Map<String, Object> common(Person p) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("accountEnabled", p.isActive());
        b.put("displayName", p.fullName());
        b.put("givenName", p.getFirstName());
        b.put("surname", p.getLastName());
        String employeeId = entraEmployeeId(p.getId());
        if (!employeeId.isBlank()) {
            b.put("employeeId", employeeId);
        }
        if (p.getJobTitle() != null) {
            b.put("jobTitle", p.getJobTitle());
        }
        if (department(p) != null) {
            b.put("department", department(p));
        }
        return b;
    }

    /** Creation payload: additionally UPN/mailNickname/initial password. NO {@code mail}/mailbox. */
    static Map<String, Object> createBody(Person p) {
        Map<String, Object> b = common(p);
        b.put("userPrincipalName", p.getEmail());
        b.put("mailNickname", localPart(p.getEmail()));
        b.put("passwordProfile", Map.of(
                "forceChangePasswordNextSignIn", false,
                "password", initialPassword()));
        return b;
    }

    /** Update payload: UPN/mailNickname/password stay stable. */
    static Map<String, Object> updateBody(Person p) {
        return common(p);
    }

    private static String localPart(String email) {
        int at = email.indexOf('@');
        return at < 0 ? email : email.substring(0, at);
    }

    /**
     * Entra limits {@code employeeId} to 1-16 characters. All ACME person ids start with the
     * redundant type prefix {@code u-}; stripping it keeps even the longest
     * ({@code u-controlling-lead} -&gt; {@code controlling-lead}, 16) within the limit and stays
     * reversible. The {@code substring} bound is a defensive upper limit (it does not kick in for
     * the ACME ids).
     */
    private static String entraEmployeeId(String personId) {
        String id = personId.startsWith("u-") ? personId.substring(2) : personId;
        return id.length() > 16 ? id.substring(0, 16) : id;
    }

    /**
     * Strong initial password (all four character classes). No interactive login is used here,
     * so sign-in is governed by {@code accountEnabled}. Only set on creation.
     */
    private static String initialPassword() {
        return "Ac1!" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
