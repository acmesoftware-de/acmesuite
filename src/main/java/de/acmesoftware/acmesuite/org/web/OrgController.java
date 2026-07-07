package de.acmesoftware.acmesuite.org.web;

import de.acmesoftware.acmesuite.org.OrgDirectory;
import de.acmesoftware.acmesuite.org.OrgViews.CostCenterView;
import de.acmesoftware.acmesuite.org.OrgViews.LegalEntityView;
import de.acmesoftware.acmesuite.org.OrgViews.OrgUnitView;
import de.acmesoftware.acmesuite.org.OrgViews.PersonView;
import de.acmesoftware.acmesuite.org.OrgViews.PowerOfAttorneyView;
import de.acmesoftware.acmesuite.org.OrgViews.RoleAssignmentView;
import de.acmesoftware.acmesuite.org.OrgViews.RoleView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only HTTP API onto the ACME Group's company model — for browsing the organization
 * (legal entities, org units, persons, roles, cost centers, powers of attorney).
 */
@RestController
@RequestMapping("/api/org")
public class OrgController {

    private final OrgDirectory org;

    public OrgController(OrgDirectory org) {
        this.org = org;
    }

    @GetMapping("/legal-entities")
    public List<LegalEntityView> legalEntities() {
        return org.legalEntities();
    }

    @GetMapping("/legal-entities/roots")
    public List<LegalEntityView> groupRoots() {
        return org.groupRoots();
    }

    @GetMapping("/legal-entities/{id}")
    public ResponseEntity<LegalEntityView> legalEntity(@PathVariable String id) {
        return ResponseEntity.of(org.legalEntity(id));
    }

    @GetMapping("/legal-entities/{id}/org-units")
    public List<OrgUnitView> orgUnits(@PathVariable String id) {
        return org.orgUnitsOf(id);
    }

    @GetMapping("/legal-entities/{id}/powers-of-attorney")
    public List<PowerOfAttorneyView> entityPowers(@PathVariable String id) {
        return org.powersOfAttorneyForEntity(id);
    }

    /** Who may sign for the specified amount for this legal entity on the given date? */
    @GetMapping("/legal-entities/{id}/signatories")
    public List<PowerOfAttorneyView> signatories(
            @PathVariable String id,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "EUR") String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return org.signatoriesFor(id, amount, currency, on == null ? LocalDate.now() : on);
    }

    @GetMapping("/org-units/{id}")
    public ResponseEntity<OrgUnitView> orgUnit(@PathVariable String id) {
        return ResponseEntity.of(org.orgUnit(id));
    }

    @GetMapping("/persons")
    public List<PersonView> persons() {
        return org.persons();
    }

    @GetMapping("/persons/{id}")
    public ResponseEntity<PersonView> person(@PathVariable String id) {
        return ResponseEntity.of(org.person(id));
    }

    @GetMapping("/persons/{id}/roles")
    public List<RoleAssignmentView> personRoles(@PathVariable String id) {
        return org.rolesOf(id);
    }

    @GetMapping("/persons/{id}/powers-of-attorney")
    public List<PowerOfAttorneyView> personPowers(@PathVariable String id) {
        return org.powersOfAttorneyOf(id);
    }

    @GetMapping("/roles")
    public List<RoleView> roles() {
        return org.roles();
    }

    @GetMapping("/cost-centers")
    public List<CostCenterView> costCenters() {
        return org.costCenters();
    }
}
