package de.acmesoftware.acmesuite.org.hr;

import de.acmesoftware.acmesuite.org.domain.Absence;
import de.acmesoftware.acmesuite.org.domain.AbsenceRepository;
import de.acmesoftware.acmesuite.org.domain.AbsenceStatus;
import de.acmesoftware.acmesuite.org.domain.ApplicantStage;
import de.acmesoftware.acmesuite.org.domain.CompensationType;
import de.acmesoftware.acmesuite.org.domain.AbsenceType;
import de.acmesoftware.acmesuite.org.domain.ApprovalLimit;
import de.acmesoftware.acmesuite.org.domain.ApprovalLimitRepository;
import de.acmesoftware.acmesuite.org.domain.LegalEntity;
import de.acmesoftware.acmesuite.org.domain.LegalEntityRepository;
import de.acmesoftware.acmesuite.org.domain.OrgUnit;
import de.acmesoftware.acmesuite.org.domain.OrgUnitRepository;
import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorney;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorneyRepository;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorneyType;
import de.acmesoftware.acmesuite.org.domain.SignatureRule;
import de.acmesoftware.acmesuite.org.hr.HrViews.AbsenceView;
import de.acmesoftware.acmesuite.org.hr.HrViews.ApplicantView;
import de.acmesoftware.acmesuite.org.hr.HrViews.ApprovalLimitView;
import de.acmesoftware.acmesuite.org.hr.HrViews.EmployeeView;
import de.acmesoftware.acmesuite.org.hr.HrViews.MoneyView;
import de.acmesoftware.acmesuite.org.hr.HrViews.PayrollSummaryView;
import de.acmesoftware.acmesuite.org.hr.HrViews.PowerOfAttorneyView;
import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** ACMEhr application logic: read employees, plan and maintain absences (vacation/sick leave). */
@Service
@Transactional
public class HrService {

    private final PersonRepository persons;
    private final AbsenceRepository absences;
    private final PowerOfAttorneyRepository powers;
    private final LegalEntityRepository legalEntities;
    private final ApprovalLimitRepository approvalLimits;
    private final OrgUnitRepository orgUnits;
    private final org.springframework.context.ApplicationEventPublisher events;

    public HrService(PersonRepository persons, AbsenceRepository absences, PowerOfAttorneyRepository powers,
                     LegalEntityRepository legalEntities, ApprovalLimitRepository approvalLimits,
                     OrgUnitRepository orgUnits,
                     org.springframework.context.ApplicationEventPublisher events) {
        this.persons = persons;
        this.absences = absences;
        this.powers = powers;
        this.legalEntities = legalEntities;
        this.approvalLimits = approvalLimits;
        this.orgUnits = orgUnits;
        this.events = events;
    }

    // ── Hiring of applicants ──

    /** Kicks off the hiring: creates a hiring folder (HR officer → HR management → managing director). */
    public EmployeeView requestHire(String applicantId, long day) {
        Person p = persons.findById(applicantId).orElseThrow(() -> notFound("Person " + applicantId + " unknown"));
        if (!p.isApplicant()) {
            throw unprocessable(p.fullName() + " is already employed");
        }
        if (p.getHireBlockedUntilDay() > day) {
            throw unprocessable(p.fullName() + " ist nach Ablehnung noch gesperrt (bis Tag "
                    + p.getHireBlockedUntilDay() + ")");
        }
        events.publishEvent(new de.acmesoftware.acmesuite.shared.ManualApprovalRequested(
                applicantId, "Hire " + p.fullName(), 0, "HIRE"));
        return EmployeeView.of(p);
    }

    /** Contract concluded → the applicant is hired (becomes an employee). Tolerant of foreign refs. */
    public void hireApplicant(String applicantId) {
        persons.findById(applicantId).ifPresent(p -> {
            if (p.isApplicant()) {
                p.setApplicant(false);
                p.setActive(true);
                p.setHireBlockedUntilDay(-1);
                persons.save(p);
                String unit = p.getPrimaryOrgUnit() == null ? null : p.getPrimaryOrgUnit().getId();
                events.publishEvent(new de.acmesoftware.acmesuite.shared.PersonHired(p.getId(), p.fullName(), unit));
            }
        });
    }

    /** Hiring rejected → 30-day block. Tolerant of foreign refs. */
    public void blockApplicant(String applicantId, long untilDay) {
        persons.findById(applicantId).ifPresent(p -> {
            if (p.isApplicant()) {
                p.setHireBlockedUntilDay(untilDay);
                persons.save(p);
            }
        });
    }

    // ── Applicants (recruiting pipeline) ──
    @Transactional(readOnly = true)
    public List<ApplicantView> listApplicants(String unitId, String q) {
        String needle = q == null ? null : q.toLowerCase();
        return persons.findAll().stream()
                .filter(Person::isApplicant)
                .filter(p -> unitId == null
                        || (p.getPrimaryOrgUnit() != null && unitId.equals(p.getPrimaryOrgUnit().getId())))
                .filter(p -> needle == null
                        || p.fullName().toLowerCase().contains(needle)
                        || (p.getJobTitle() != null && p.getJobTitle().toLowerCase().contains(needle)))
                .sorted(Comparator.comparing(Person::fullName))
                .map(ApplicantView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ApplicantView> getApplicant(String id) {
        return persons.findById(id).filter(Person::isApplicant).map(ApplicantView::of);
    }

    public ApplicantView createApplicant(String firstName, String lastName, String email, String jobTitle,
                                         String targetOrgUnitId, ApplicantStage stage, Integer matchScore) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw unprocessable("firstName and lastName are required");
        }
        OrgUnit unit = targetOrgUnitId == null ? null : orgUnits.findById(targetOrgUnitId)
                .orElseThrow(() -> unprocessable("Org unit " + targetOrgUnitId + " unknown"));
        String id = "app-" + UUID.randomUUID().toString().substring(0, 12);
        Person p = new Person(id, firstName, lastName, email, jobTitle, unit);
        p.setApplicant(true);
        p.setActive(false);
        p.setRecruiting(stage == null ? ApplicantStage.NEW : stage, clampScore(matchScore), LocalDate.now());
        return ApplicantView.of(persons.save(p));
    }

    /** Move an applicant along the recruiting pipeline (Bewerber-Board drag & drop). */
    public ApplicantView updateApplicantStage(String id, ApplicantStage stage) {
        if (stage == null) {
            throw unprocessable("stage is required");
        }
        Person p = persons.findById(id).filter(Person::isApplicant)
                .orElseThrow(() -> notFound("Applicant " + id + " unknown"));
        p.setApplicantStage(stage);
        return ApplicantView.of(p);
    }

    /** Reject/remove an application — tombstoned (ADR-0010), never hard-deleted. */
    public void rejectApplicant(String id) {
        Person p = persons.findById(id).filter(Person::isApplicant)
                .orElseThrow(() -> notFound("Applicant " + id + " unknown"));
        p.tombstone(null, java.time.Instant.now());
        persons.save(p);
    }

    private static Integer clampScore(Integer score) {
        if (score == null) {
            return null;
        }
        return Math.max(0, Math.min(100, score));
    }

    // ── Employees ──
    @Transactional(readOnly = true)
    public List<EmployeeView> listEmployees(Boolean active, String unitId, String q) {
        String needle = q == null ? null : q.toLowerCase();
        return persons.findAll().stream()
                .filter(p -> active == null || p.isActive() == active)
                .filter(p -> unitId == null
                        || (p.getPrimaryOrgUnit() != null && unitId.equals(p.getPrimaryOrgUnit().getId())))
                .filter(p -> needle == null
                        || p.fullName().toLowerCase().contains(needle)
                        || (p.getEmail() != null && p.getEmail().toLowerCase().contains(needle))
                        || (p.getJobTitle() != null && p.getJobTitle().toLowerCase().contains(needle)))
                .sorted(Comparator.comparing(Person::fullName))
                .map(EmployeeView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeView> getEmployee(String id) {
        return persons.findById(id).map(EmployeeView::of);
    }

    // ── Absences ──
    @Transactional(readOnly = true)
    public List<AbsenceView> listAbsences(String personId, AbsenceType type, AbsenceStatus status,
                                          LocalDate from, LocalDate until) {
        List<Absence> base = personId != null ? absences.findByPerson_Id(personId) : absences.findAll();
        return base.stream()
                .filter(a -> type == null || a.getType() == type)
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> overlaps(a.getPeriod(), from, until))
                .sorted(Comparator.comparing((Absence a) -> a.getPeriod() == null ? LocalDate.MIN : a.getPeriod().from())
                        .reversed())
                .map(AbsenceView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AbsenceView> absencesOf(String personId) {
        return listAbsences(personId, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public Optional<AbsenceView> getAbsence(String id) {
        return absences.findById(id).map(AbsenceView::of);
    }

    public AbsenceView createAbsence(String personId, AbsenceType type, LocalDate from, LocalDate until,
                                     String substituteId, String note) {
        Person person = persons.findById(personId)
                .orElseThrow(() -> notFound("Person " + personId + " unknown"));
        Person substitute = resolveSubstitute(substituteId);
        DateRange period = period(from, until);
        AbsenceStatus status = type == AbsenceType.SICK ? AbsenceStatus.APPROVED : AbsenceStatus.PLANNED;
        String id = "abs-" + UUID.randomUUID().toString().substring(0, 12);
        String reasonKey = type.name().toLowerCase() + "-" + id.substring(4);
        Absence a = new Absence(id, person, reasonKey, type, status, substitute, period, note);
        return AbsenceView.of(absences.save(a));
    }

    public AbsenceView updateAbsence(String id, AbsenceStatus status, String substituteId,
                                     LocalDate from, LocalDate until, String note, boolean substituteGiven) {
        Absence a = absences.findById(id).orElseThrow(() -> notFound("Absence " + id + " unknown"));
        if (status != null) {
            a.changeStatus(status);
        }
        if (substituteGiven) {
            a.assignSubstitute(resolveSubstitute(substituteId));
        }
        if (from != null || until != null) {
            LocalDate f = from != null ? from : a.getPeriod().from();
            LocalDate u = until != null ? until : a.getPeriod().until();
            a.reschedule(period(f, u));
        }
        if (note != null) {
            a.setNote(note);
        }
        return AbsenceView.of(a);
    }

    public void deleteAbsence(String id) {
        if (!absences.existsById(id)) {
            throw notFound("Absence " + id + " unknown");
        }
        absences.deleteById(id);
    }

    // ── Employee overlay ──
    public EmployeeView updateEmployee(String id, String jobTitle, String managerId, Boolean active,
                                       List<String> deputyIds, List<String> assistantIds) {
        Person p = persons.findById(id).orElseThrow(() -> notFound("Person " + id + " unknown"));
        if (jobTitle != null) {
            p.updateJobTitle(jobTitle);
        }
        if (managerId != null) {
            p.assignManager(persons.findById(managerId)
                    .orElseThrow(() -> unprocessable("Manager " + managerId + " unknown")));
        }
        if (active != null) {
            p.setActive(active);
        }
        if (deputyIds != null) {
            p.replaceDelegates(deputyIds);
        }
        if (assistantIds != null) {
            p.replaceAssistants(assistantIds);
        }
        return EmployeeView.of(p);
    }

    // ── Compensation / Payroll ──

    /** Switch/adjust compensation (hourly wage ↔ salary, hourly rate). */
    public EmployeeView updateCompensation(String id, CompensationType compType, BigDecimal hourlyRate) {
        Person p = persons.findById(id).orElseThrow(() -> notFound("Person " + id + " unknown"));
        CompensationType type = compType != null ? compType : p.getCompType();
        BigDecimal rate = hourlyRate != null ? hourlyRate : p.getHourlyRate();
        if (rate.signum() < 0) {
            throw unprocessable("hourly rate must not be negative");
        }
        p.setCompensation(type, rate);
        return EmployeeView.of(p);
    }

    /** Payroll summary: salary exact (9 h × 7 days), hourly wage as a standard week (8.5 h × 5 days). */
    @Transactional(readOnly = true)
    public PayrollSummaryView payrollSummary() {
        BigDecimal salariedWeek = BigDecimal.valueOf(9 * 7);     // 63 h/week
        BigDecimal hourlyWeek = BigDecimal.valueOf(8.5 * 5);     // ~42.5 h/week (estimate)
        int salariedCount = 0;
        int hourlyCount = 0;
        BigDecimal salaried = BigDecimal.ZERO;
        BigDecimal hourly = BigDecimal.ZERO;
        for (Person p : persons.findAll()) {
            if (!p.isActive() || p.isApplicant()) {
                continue;
            }
            if (p.getCompType() == CompensationType.SALARIED) {
                salariedCount++;
                salaried = salaried.add(p.getHourlyRate().multiply(salariedWeek));
            } else {
                hourlyCount++;
                hourly = hourly.add(p.getHourlyRate().multiply(hourlyWeek));
            }
        }
        salaried = salaried.setScale(0, java.math.RoundingMode.HALF_UP);
        hourly = hourly.setScale(0, java.math.RoundingMode.HALF_UP);
        return new PayrollSummaryView(salariedCount, hourlyCount, salaried, hourly, salaried.add(hourly));
    }

    // ── Powers of attorney ──
    @Transactional(readOnly = true)
    public List<PowerOfAttorneyView> listPowers(String holderId, String legalEntityId, boolean includeRevoked) {
        List<PowerOfAttorney> base;
        if (holderId != null) {
            base = powers.findByHolder_Id(holderId);
        } else if (legalEntityId != null) {
            base = powers.findByLegalEntity_Id(legalEntityId);
        } else {
            base = powers.findAll();
        }
        return base.stream()
                .filter(p -> includeRevoked || !p.isRevoked())
                .map(PowerOfAttorneyView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PowerOfAttorneyView> powersOf(String holderId) {
        return powers.findByHolder_Id(holderId).stream().map(PowerOfAttorneyView::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PowerOfAttorneyView> getPower(String id) {
        return powers.findById(id).map(PowerOfAttorneyView::of);
    }

    public PowerOfAttorneyView grantPower(String holderId, String legalEntityId, PowerOfAttorneyType type,
                                          SignatureRule signatureRule, MoneyView limit, String scope,
                                          LocalDate validFrom, LocalDate validUntil) {
        Person holder = persons.findById(holderId)
                .orElseThrow(() -> notFound("Person " + holderId + " unknown"));
        LegalEntity entity = legalEntities.findById(legalEntityId)
                .orElseThrow(() -> unprocessable("Legal entity " + legalEntityId + " unknown"));
        if (validFrom == null) {
            throw unprocessable("validFrom is required");
        }
        String id = "poa-" + UUID.randomUUID().toString().substring(0, 12);
        PowerOfAttorney poa = new PowerOfAttorney(id, holder, entity, type, signatureRule, money(limit),
                scope, new DateRange(validFrom, validUntil));
        return PowerOfAttorneyView.of(powers.save(poa));
    }

    public PowerOfAttorneyView revokePower(String id) {
        PowerOfAttorney poa = powers.findById(id).orElseThrow(() -> notFound("Power of attorney " + id + " unknown"));
        poa.revoke();
        return PowerOfAttorneyView.of(poa);
    }

    @Transactional(readOnly = true)
    public List<PowerOfAttorneyView> signatories(String legalEntityId, BigDecimal amount, String currency,
                                                 LocalDate on) {
        Money want = new Money(amount, currency == null ? "EUR" : currency);
        LocalDate day = on == null ? LocalDate.now() : on;
        return powers.findByLegalEntity_Id(legalEntityId).stream()
                .filter(p -> p.covers(want, day))
                .map(PowerOfAttorneyView::of)
                .toList();
    }

    // ── Approval limits ──
    @Transactional(readOnly = true)
    public ApprovalLimitView effectiveApprovalLimit(String personId, String legalEntityId, LocalDate on) {
        Person p = persons.findById(personId).orElseThrow(() -> notFound("Person " + personId + " unknown"));
        LocalDate day = on == null ? LocalDate.now() : on;
        // 1) Explicit limit (specific before global), if valid on the reference date.
        Optional<ApprovalLimit> explicit = legalEntityId != null
                ? approvalLimits.findByPerson_IdAndLegalEntity_Id(personId, legalEntityId)
                : approvalLimits.findByPerson_IdAndLegalEntityIsNull(personId).stream().findFirst();
        if (explicit.isEmpty() && legalEntityId != null) {
            explicit = approvalLimits.findByPerson_IdAndLegalEntityIsNull(personId).stream().findFirst();
        }
        if (explicit.isPresent() && active(explicit.get().getValidity(), day)) {
            return ApprovalLimitView.explicit(explicit.get());
        }
        // 2) Derive from active powers of attorney: unlimited beats everything, otherwise the highest limit.
        PowerOfAttorney best = null;
        for (PowerOfAttorney poa : powers.findByHolder_Id(personId)) {
            if (poa.isRevoked() || !active(poa.getValidity(), day)) {
                continue;
            }
            if (legalEntityId != null && !poa.getLegalEntity().getId().equals(legalEntityId)) {
                continue;
            }
            if (best == null || better(poa, best)) {
                best = poa;
            }
        }
        if (best != null) {
            return new ApprovalLimitView(p.getId(), p.fullName(), best.getLegalEntity().getId(),
                    MoneyView.of(best.getLimit()), best.getSignatureRule().name(), "POWER_OF_ATTORNEY",
                    best.getValidity());
        }
        // 3) No signing authority.
        return new ApprovalLimitView(p.getId(), p.fullName(), legalEntityId,
                new MoneyView(BigDecimal.ZERO, "EUR", false), null, "POWER_OF_ATTORNEY", null);
    }

    @Transactional(readOnly = true)
    public List<ApprovalLimitView> listApprovalLimits(String personId) {
        List<ApprovalLimit> base = personId != null ? approvalLimits.findByPerson_Id(personId) : approvalLimits.findAll();
        return base.stream().map(ApprovalLimitView::explicit).toList();
    }

    public ApprovalLimitView setApprovalLimit(String personId, String legalEntityId, MoneyView maxAmount,
                                              LocalDate validFrom, LocalDate validUntil) {
        Person person = persons.findById(personId)
                .orElseThrow(() -> notFound("Person " + personId + " unknown"));
        LegalEntity entity = legalEntityId == null ? null : legalEntities.findById(legalEntityId)
                .orElseThrow(() -> unprocessable("Legal entity " + legalEntityId + " unknown"));
        if (maxAmount == null) {
            throw unprocessable("maxAmount is required");
        }
        DateRange validity = new DateRange(validFrom, validUntil);
        Optional<ApprovalLimit> existing = entity != null
                ? approvalLimits.findByPerson_IdAndLegalEntity_Id(personId, entity.getId())
                : approvalLimits.findByPerson_IdAndLegalEntityIsNull(personId).stream().findFirst();
        ApprovalLimit limit = existing.orElseGet(() -> new ApprovalLimit(
                "lim-" + personId + (entity == null ? "" : "-" + entity.getId()), person, entity,
                money(maxAmount), validity));
        limit.update(money(maxAmount), validity);
        return ApprovalLimitView.explicit(approvalLimits.save(limit));
    }

    // ── helpers ──
    private static Money money(MoneyView m) {
        return m == null || m.unlimited() ? Money.unlimited() : new Money(m.amount(), m.currency());
    }

    private static boolean active(DateRange v, LocalDate on) {
        return v == null || v.isActiveOn(on);
    }

    /** "Better" = unlimited, otherwise a higher limit (same currency assumed). */
    private static boolean better(PowerOfAttorney candidate, PowerOfAttorney current) {
        Money c = candidate.getLimit();
        Money b = current.getLimit();
        if (c == null || c.isUnlimited()) {
            return true;
        }
        if (b == null || b.isUnlimited()) {
            return false;
        }
        return c.amount().compareTo(b.amount()) > 0;
    }
    private Person resolveSubstitute(String substituteId) {
        if (substituteId == null) {
            return null;
        }
        return persons.findById(substituteId)
                .orElseThrow(() -> unprocessable("Substitute " + substituteId + " unknown"));
    }

    private DateRange period(LocalDate from, LocalDate until) {
        if (from == null || until == null) {
            throw unprocessable("from and until are required");
        }
        if (until.isBefore(from)) {
            throw unprocessable("until liegt vor from");
        }
        return new DateRange(from, until);
    }

    private static boolean overlaps(DateRange p, LocalDate from, LocalDate until) {
        if (p == null || (from == null && until == null)) {
            return true;
        }
        LocalDate pf = p.from() == null ? LocalDate.MIN : p.from();
        LocalDate pu = p.until() == null ? LocalDate.MAX : p.until();
        if (from != null && pu.isBefore(from)) {
            return false;
        }
        return until == null || !pf.isAfter(until);
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private static ResponseStatusException unprocessable(String msg) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, msg);
    }
}
