package de.acmesoftware.acmesuite.org;

import de.acmesoftware.acmesuite.org.OrgViews.CostCenterView;
import de.acmesoftware.acmesuite.org.OrgViews.LegalEntityView;
import de.acmesoftware.acmesuite.org.OrgViews.OrgUnitView;
import de.acmesoftware.acmesuite.org.OrgViews.PersonView;
import de.acmesoftware.acmesuite.org.OrgViews.PowerOfAttorneyView;
import de.acmesoftware.acmesuite.org.OrgViews.RoleAssignmentView;
import de.acmesoftware.acmesuite.org.OrgViews.RoleView;
import de.acmesoftware.acmesuite.org.domain.CostCenterRepository;
import de.acmesoftware.acmesuite.org.domain.LegalEntityRepository;
import de.acmesoftware.acmesuite.org.domain.OrgUnitRepository;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorneyRepository;
import de.acmesoftware.acmesuite.org.domain.RoleAssignmentRepository;
import de.acmesoftware.acmesuite.org.domain.RoleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the {@code org} module: read access to the company model.
 * Other modules address the organization exclusively through this.
 *
 * <p>This phase is read-only — the model is populated via a Flyway seed. Write operations
 * (onboarding, granting/revoking power of attorney) will arrive with the process module.
 */
@Service
@Transactional(readOnly = true)
public class OrgDirectory {

    private final LegalEntityRepository legalEntities;
    private final OrgUnitRepository orgUnits;
    private final PersonRepository persons;
    private final RoleRepository roles;
    private final RoleAssignmentRepository roleAssignments;
    private final CostCenterRepository costCenters;
    private final PowerOfAttorneyRepository powersOfAttorney;

    public OrgDirectory(LegalEntityRepository legalEntities, OrgUnitRepository orgUnits,
                        PersonRepository persons, RoleRepository roles,
                        RoleAssignmentRepository roleAssignments, CostCenterRepository costCenters,
                        PowerOfAttorneyRepository powersOfAttorney) {
        this.legalEntities = legalEntities;
        this.orgUnits = orgUnits;
        this.persons = persons;
        this.roles = roles;
        this.roleAssignments = roleAssignments;
        this.costCenters = costCenters;
        this.powersOfAttorney = powersOfAttorney;
    }

    // --- Legal entities ---------------------------------------------------

    public List<LegalEntityView> legalEntities() {
        return legalEntities.findAll().stream().map(LegalEntityView::of).toList();
    }

    public Optional<LegalEntityView> legalEntity(String id) {
        return legalEntities.findById(id).map(LegalEntityView::of);
    }

    public List<LegalEntityView> groupRoots() {
        return legalEntities.findByParentIsNull().stream().map(LegalEntityView::of).toList();
    }

    // --- Organizational units ---------------------------------------------

    public List<OrgUnitView> orgUnitsOf(String legalEntityId) {
        return orgUnits.findByLegalEntity_Id(legalEntityId).stream().map(OrgUnitView::of).toList();
    }

    public Optional<OrgUnitView> orgUnit(String id) {
        return orgUnits.findById(id).map(OrgUnitView::of);
    }

    // --- Persons ----------------------------------------------------------

    public List<PersonView> persons() {
        return persons.findAll().stream().map(PersonView::of).toList();
    }

    public Optional<PersonView> person(String id) {
        return persons.findById(id).map(PersonView::of);
    }

    /** All persons whose primary membership is this unit. */
    public List<PersonView> personsOf(String unitKey) {
        return persons.findByPrimaryOrgUnit_Id(unitKey).stream().map(PersonView::of).toList();
    }

    /**
     * Lead of a unit = the person whose manager is not in the same unit (reports outward/upward).
     * Used for resolving approvers in contract routing.
     */
    public Optional<PersonView> leadOf(String unitKey) {
        var inUnit = persons.findByPrimaryOrgUnit_Id(unitKey);
        var keysInUnit = inUnit.stream().map(de.acmesoftware.acmesuite.org.domain.Person::getId).collect(java.util.stream.Collectors.toSet());
        return inUnit.stream()
                .filter(p -> p.getManager() == null || !keysInUnit.contains(p.getManager().getId()))
                .map(PersonView::of)
                .findFirst();
    }

    // --- Roles ------------------------------------------------------------

    public List<RoleView> roles() {
        return roles.findAll().stream().map(RoleView::of).toList();
    }

    public List<RoleAssignmentView> rolesOf(String personId) {
        return roleAssignments.findByPerson_Id(personId).stream().map(RoleAssignmentView::of).toList();
    }

    // --- Cost centers -----------------------------------------------------

    public List<CostCenterView> costCenters() {
        return costCenters.findAll().stream().map(CostCenterView::of).toList();
    }

    // --- Powers of attorney -----------------------------------------------

    public List<PowerOfAttorneyView> powersOfAttorneyOf(String personId) {
        return powersOfAttorney.findByHolder_Id(personId).stream().map(PowerOfAttorneyView::of).toList();
    }

    public List<PowerOfAttorneyView> powersOfAttorneyForEntity(String legalEntityId) {
        return powersOfAttorney.findByLegalEntity_Id(legalEntityId).stream()
                .map(PowerOfAttorneyView::of).toList();
    }

    /**
     * Returns the powers of attorney that, on the given date, cover the specified amount for a legal
     * entity — the basis for the later approval/signing logic in the process module.
     */
    public List<PowerOfAttorneyView> signatoriesFor(String legalEntityId, java.math.BigDecimal amount,
                                                    String currency, LocalDate on) {
        var money = new de.acmesoftware.acmesuite.shared.Money(amount, currency);
        return powersOfAttorney.findByLegalEntity_Id(legalEntityId).stream()
                .filter(p -> p.covers(money, on))
                .map(PowerOfAttorneyView::of)
                .toList();
    }
}
