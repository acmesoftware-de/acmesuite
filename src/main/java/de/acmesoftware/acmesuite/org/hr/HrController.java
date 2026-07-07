package de.acmesoftware.acmesuite.org.hr;

import de.acmesoftware.acmesuite.org.OrgDirectory;
import de.acmesoftware.acmesuite.org.OrgViews.RoleAssignmentView;
import de.acmesoftware.acmesuite.org.OrgViews.RoleView;
import de.acmesoftware.acmesuite.org.domain.AbsenceStatus;
import de.acmesoftware.acmesuite.org.domain.AbsenceType;
import de.acmesoftware.acmesuite.org.domain.CompensationType;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorneyType;
import de.acmesoftware.acmesuite.org.domain.SignatureRule;
import de.acmesoftware.acmesuite.org.hr.HrViews.AbsenceView;
import de.acmesoftware.acmesuite.org.hr.HrViews.ApprovalLimitView;
import de.acmesoftware.acmesuite.org.hr.HrViews.EmployeeView;
import de.acmesoftware.acmesuite.org.hr.HrViews.MoneyView;
import de.acmesoftware.acmesuite.org.hr.HrViews.PayrollSummaryView;
import de.acmesoftware.acmesuite.org.hr.HrViews.PowerOfAttorneyView;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ACMEhr HTTP API (write/planning surface): employees + absences (vacation/sick leave).
 * Implements {@code api/acme-hr.yaml}; complements the read-only {@code /api/org} browsing.
 */
@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final HrService hr;
    private final OrgDirectory org;

    public HrController(HrService hr, OrgDirectory org) {
        this.hr = hr;
        this.org = org;
    }

    // ── Employees ──
    @GetMapping("/employees")
    public List<EmployeeView> employees(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String unitId,
            @RequestParam(required = false) String q) {
        return hr.listEmployees(active, unitId, q);
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeView> employee(@PathVariable String id) {
        return ResponseEntity.of(hr.getEmployee(id));
    }

    @PatchMapping("/employees/{id}")
    public EmployeeView updateEmployee(@PathVariable String id, @RequestBody EmployeeUpdateReq req) {
        return hr.updateEmployee(id, req.jobTitle(), req.managerId(), req.active(),
                req.deputyIds(), req.assistantIds());
    }

    @PatchMapping("/employees/{id}/compensation")
    public EmployeeView updateCompensation(@PathVariable String id, @RequestBody CompensationReq req) {
        return hr.updateCompensation(id, req.compType(), req.hourlyRate());
    }

    /** Hire an applicant → hiring folder (HR officer → HR management → managing director). */
    @PostMapping("/employees/{id}/hire")
    public EmployeeView hire(@PathVariable String id, @RequestParam(defaultValue = "0") long day) {
        return hr.requestHire(id, day);
    }

    @GetMapping("/payroll")
    public PayrollSummaryView payroll() {
        return hr.payrollSummary();
    }

    @GetMapping("/employees/{id}/absences")
    public List<AbsenceView> employeeAbsences(@PathVariable String id) {
        return hr.absencesOf(id);
    }

    @GetMapping("/employees/{id}/roles")
    public List<RoleAssignmentView> employeeRoles(@PathVariable String id) {
        return org.rolesOf(id);
    }

    @GetMapping("/employees/{id}/powers-of-attorney")
    public List<PowerOfAttorneyView> employeePowers(@PathVariable String id) {
        return hr.powersOf(id);
    }

    @GetMapping("/employees/{id}/approval-limit")
    public ApprovalLimitView employeeApprovalLimit(
            @PathVariable String id,
            @RequestParam(required = false) String legalEntityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return hr.effectiveApprovalLimit(id, legalEntityId, on);
    }

    @GetMapping("/roles")
    public List<RoleView> roles() {
        return org.roles();
    }

    // ── Absences ──
    @GetMapping("/absences")
    public List<AbsenceView> absences(
            @RequestParam(required = false) String personId,
            @RequestParam(required = false) AbsenceType type,
            @RequestParam(required = false) AbsenceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until) {
        return hr.listAbsences(personId, type, status, from, until);
    }

    @PostMapping("/absences")
    public ResponseEntity<AbsenceView> createAbsence(@RequestBody AbsenceCreateReq req) {
        AbsenceView v = hr.createAbsence(req.personId(), req.type(), req.from(), req.until(),
                req.substituteId(), req.note());
        return ResponseEntity.created(URI.create("/api/hr/absences/" + v.id())).body(v);
    }

    @GetMapping("/absences/{id}")
    public ResponseEntity<AbsenceView> absence(@PathVariable String id) {
        return ResponseEntity.of(hr.getAbsence(id));
    }

    @PatchMapping("/absences/{id}")
    public AbsenceView updateAbsence(@PathVariable String id, @RequestBody AbsenceUpdateReq req) {
        return hr.updateAbsence(id, req.status(), req.substituteId(), req.from(), req.until(), req.note(),
                req.substituteId() != null);
    }

    @DeleteMapping("/absences/{id}")
    public ResponseEntity<Void> deleteAbsence(@PathVariable String id) {
        hr.deleteAbsence(id);
        return ResponseEntity.noContent().build();
    }

    // ── Powers of attorney ──
    @GetMapping("/powers-of-attorney")
    public List<PowerOfAttorneyView> powers(
            @RequestParam(required = false) String holderId,
            @RequestParam(required = false) String legalEntityId,
            @RequestParam(defaultValue = "false") boolean includeRevoked) {
        return hr.listPowers(holderId, legalEntityId, includeRevoked);
    }

    @PostMapping("/powers-of-attorney")
    public ResponseEntity<PowerOfAttorneyView> grantPower(@RequestBody PowerGrantReq req) {
        PowerOfAttorneyView v = hr.grantPower(req.holderId(), req.legalEntityId(), req.type(), req.signatureRule(),
                req.limit(), req.scope(), req.validFrom(), req.validUntil());
        return ResponseEntity.created(URI.create("/api/hr/powers-of-attorney/" + v.id())).body(v);
    }

    @GetMapping("/powers-of-attorney/{id}")
    public ResponseEntity<PowerOfAttorneyView> power(@PathVariable String id) {
        return ResponseEntity.of(hr.getPower(id));
    }

    @PutMapping("/powers-of-attorney/{id}/revocation")
    public PowerOfAttorneyView revokePower(@PathVariable String id) {
        return hr.revokePower(id);
    }

    @GetMapping("/legal-entities/{legalEntityId}/signatories")
    public List<PowerOfAttorneyView> signatories(
            @PathVariable String legalEntityId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "EUR") String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return hr.signatories(legalEntityId, amount, currency, on);
    }

    // ── Approval limits ──
    @GetMapping("/approval-limits")
    public List<ApprovalLimitView> approvalLimits(@RequestParam(required = false) String personId) {
        return hr.listApprovalLimits(personId);
    }

    @PutMapping("/approval-limits")
    public ApprovalLimitView setApprovalLimit(@RequestBody ApprovalLimitSetReq req) {
        return hr.setApprovalLimit(req.personId(), req.legalEntityId(), req.maxAmount(),
                req.validFrom(), req.validUntil());
    }

    public record EmployeeUpdateReq(String jobTitle, String managerId, Boolean active,
                                    List<String> deputyIds, List<String> assistantIds) {
    }

    public record CompensationReq(CompensationType compType, java.math.BigDecimal hourlyRate) {
    }

    public record AbsenceCreateReq(String personId, AbsenceType type, LocalDate from, LocalDate until,
                                   String substituteId, String note) {
    }

    public record AbsenceUpdateReq(AbsenceStatus status, String substituteId, LocalDate from, LocalDate until,
                                   String note) {
    }

    public record PowerGrantReq(String holderId, String legalEntityId, PowerOfAttorneyType type,
                                SignatureRule signatureRule, MoneyView limit, String scope,
                                LocalDate validFrom, LocalDate validUntil) {
    }

    public record ApprovalLimitSetReq(String personId, String legalEntityId, MoneyView maxAmount,
                                      LocalDate validFrom, LocalDate validUntil) {
    }
}
