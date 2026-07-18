package de.acmesoftware.acmesuite.org.hr;

import de.acmesoftware.acmesuite.org.domain.Absence;
import de.acmesoftware.acmesuite.org.domain.ApprovalLimit;
import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorney;
import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Serializable ACMEhr views (DTOs) — congruent with the OpenAPI contract {@code api/acme-hr.yaml}. */
public final class HrViews {

    private HrViews() {
    }

    public record EmployeeView(String id, String firstName, String lastName, String fullName, String email,
                               String jobTitle, boolean active, boolean applicant, String primaryOrgUnitId,
                               String primaryOrgUnitName,
                               String managerId, List<String> deputyIds, List<String> assistantIds,
                               List<String> secondaryUnitIds, String compType, java.math.BigDecimal hourlyRate) {
        public static EmployeeView of(Person p) {
            return new EmployeeView(p.getId(), p.getFirstName(), p.getLastName(), p.fullName(), p.getEmail(),
                    p.getJobTitle(), p.isActive(), p.isApplicant(),
                    p.getPrimaryOrgUnit() == null ? null : p.getPrimaryOrgUnit().getId(),
                    p.getPrimaryOrgUnit() == null ? null : p.getPrimaryOrgUnit().getName(),
                    p.getManager() == null ? null : p.getManager().getId(),
                    List.copyOf(p.getDelegateIds()), List.copyOf(p.getAssistantIds()),
                    List.copyOf(p.getSecondaryUnitIds()),
                    p.getCompType().name(), p.getHourlyRate());
        }
    }

    /** Applicant projection (applicant=true) incl. recruiting pipeline stage + fit score. */
    public record ApplicantView(String id, String firstName, String lastName, String fullName, String email,
                                String jobTitle, String targetOrgUnitId, String targetOrgUnitName,
                                LocalDate appliedOn, String stage, Integer matchScore) {
        public static ApplicantView of(Person p) {
            return new ApplicantView(p.getId(), p.getFirstName(), p.getLastName(), p.fullName(), p.getEmail(),
                    p.getJobTitle(),
                    p.getPrimaryOrgUnit() == null ? null : p.getPrimaryOrgUnit().getId(),
                    p.getPrimaryOrgUnit() == null ? null : p.getPrimaryOrgUnit().getName(),
                    p.getAppliedOn(),
                    p.getApplicantStage() == null ? null : p.getApplicantStage().name(),
                    p.getMatchScore());
        }
    }

    /** Payroll summary per compensation class (weekly estimate; hourly wage ~ standard week). */
    public record PayrollSummaryView(int salariedCount, int hourlyCount,
                                     java.math.BigDecimal weeklySalariedEur, java.math.BigDecimal weeklyHourlyEur,
                                     java.math.BigDecimal weeklyTotalEur) {
    }

    public record AbsenceView(String id, String personId, String personName, String type, String status,
                              DateRange period, String substituteId, String substituteName, String reasonKey,
                              String note, Integer workingDays) {
        public static AbsenceView of(Absence a) {
            Person sub = a.getSubstitute();
            return new AbsenceView(a.getId(), a.getPerson().getId(), a.getPerson().fullName(),
                    a.getType().name(), a.getStatus().name(), a.getPeriod(),
                    sub == null ? null : sub.getId(), sub == null ? null : sub.fullName(),
                    a.getReasonKey(), a.getNote(), workingDays(a.getPeriod()));
        }

        private static Integer workingDays(DateRange p) {
            if (p == null || p.from() == null || p.until() == null) {
                return null;
            }
            int days = 0;
            for (LocalDate d = p.from(); !d.isAfter(p.until()); d = d.plusDays(1)) {
                if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    days++;
                }
            }
            return days;
        }
    }

    public record MoneyView(java.math.BigDecimal amount, String currency, boolean unlimited) {
        public static MoneyView of(Money m) {
            boolean unl = m == null || m.isUnlimited();
            return new MoneyView(unl ? null : m.amount(), unl ? null : m.currency(), unl);
        }
    }

    public record PowerOfAttorneyView(String id, String holderId, String holderName, String legalEntityId,
                                      String type, String signatureRule, MoneyView limit, String scope,
                                      DateRange validity, boolean revoked) {
        public static PowerOfAttorneyView of(PowerOfAttorney p) {
            return new PowerOfAttorneyView(p.getId(), p.getHolder().getId(), p.getHolder().fullName(),
                    p.getLegalEntity().getId(), p.getType().name(), p.getSignatureRule().name(),
                    MoneyView.of(p.getLimit()), p.getScope(), p.getValidity(), p.isRevoked());
        }
    }

    public record ApprovalLimitView(String personId, String personName, String legalEntityId, MoneyView maxAmount,
                                    String signatureRule, String source, DateRange validity) {
        public static ApprovalLimitView explicit(ApprovalLimit l) {
            return new ApprovalLimitView(l.getPerson().getId(), l.getPerson().fullName(),
                    l.getLegalEntity() == null ? null : l.getLegalEntity().getId(),
                    MoneyView.of(l.getMaxAmount()), null, "EXPLICIT", l.getValidity());
        }
    }
}
