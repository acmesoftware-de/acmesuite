package de.acmesoftware.acmesuite.org;

import de.acmesoftware.acmesuite.org.domain.CostCenter;
import de.acmesoftware.acmesuite.org.domain.LegalEntity;
import de.acmesoftware.acmesuite.org.domain.OrgUnit;
import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorney;
import de.acmesoftware.acmesuite.org.domain.Role;
import de.acmesoftware.acmesuite.org.domain.RoleAssignment;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Externally visible, serializable views (DTOs) of the org model. Decouples the JPA aggregates
 * from the API and avoids lazy-loading/cycle problems in the JSON output.
 */
public final class OrgViews {

    private OrgViews() {
    }

    public record LegalEntityView(String id, String legalName, String type, String parentId,
                                  String country, String registrationNumber, String platformTenantKey) {
        public static LegalEntityView of(LegalEntity e) {
            return new LegalEntityView(e.getId(), e.getLegalName(), e.getType().name(),
                    e.getParent() == null ? null : e.getParent().getId(),
                    e.getCountry(), e.getRegistrationNumber(), e.getPlatformTenantKey());
        }
    }

    public record OrgUnitView(String id, String name, String type, String legalEntityId, String parentId) {
        public static OrgUnitView of(OrgUnit u) {
            return new OrgUnitView(u.getId(), u.getName(), u.getType().name(),
                    u.getLegalEntity().getId(),
                    u.getParent() == null ? null : u.getParent().getId());
        }
    }

    public record PersonView(String id, String fullName, String email, String jobTitle, boolean active,
                             boolean applicant, String primaryOrgUnitId, String managerId,
                             java.util.List<String> delegateIds,
                             java.util.List<String> assistantIds, java.util.List<String> secondaryUnitIds) {
        public static PersonView of(Person p) {
            return new PersonView(p.getId(), p.fullName(), p.getEmail(), p.getJobTitle(), p.isActive(),
                    p.isApplicant(),
                    p.getPrimaryOrgUnit() == null ? null : p.getPrimaryOrgUnit().getId(),
                    p.getManager() == null ? null : p.getManager().getId(),
                    java.util.List.copyOf(p.getDelegateIds()),
                    java.util.List.copyOf(p.getAssistantIds()),
                    java.util.List.copyOf(p.getSecondaryUnitIds()));
        }
    }

    public record RoleView(String id, String title, String kind, String description) {
        public static RoleView of(Role r) {
            return new RoleView(r.getId(), r.getTitle(), r.getKind().name(), r.getDescription());
        }
    }

    public record RoleAssignmentView(String id, String personId, String personName, String roleId,
                                     String roleTitle, String orgUnitId, LocalDate validFrom, LocalDate validUntil) {
        public static RoleAssignmentView of(RoleAssignment a) {
            var v = a.getValidity();
            return new RoleAssignmentView(a.getId(), a.getPerson().getId(), a.getPerson().fullName(),
                    a.getRole().getId(), a.getRole().getTitle(),
                    a.getOrgUnit() == null ? null : a.getOrgUnit().getId(),
                    v == null ? null : v.from(), v == null ? null : v.until());
        }
    }

    public record CostCenterView(String id, String name, String orgUnitId, String responsiblePersonId,
                                 BigDecimal annualBudget, String currency) {
        public static CostCenterView of(CostCenter c) {
            var b = c.getAnnualBudget();
            return new CostCenterView(c.getId(), c.getName(), c.getOrgUnit().getId(),
                    c.getResponsible() == null ? null : c.getResponsible().getId(),
                    b == null ? null : b.amount(), b == null ? null : b.currency());
        }
    }

    public record PowerOfAttorneyView(String id, String holderId, String holderName, String legalEntityId,
                                      String type, String signatureRule, BigDecimal limitAmount, String limitCurrency,
                                      boolean unlimited, String scope, LocalDate validFrom, LocalDate validUntil,
                                      boolean revoked) {
        public static PowerOfAttorneyView of(PowerOfAttorney p) {
            var l = p.getLimit();
            var v = p.getValidity();
            boolean unlimited = l == null || l.isUnlimited();
            return new PowerOfAttorneyView(p.getId(), p.getHolder().getId(), p.getHolder().fullName(),
                    p.getLegalEntity().getId(), p.getType().name(), p.getSignatureRule().name(),
                    unlimited ? null : l.amount(), unlimited ? null : l.currency(), unlimited,
                    p.getScope(), v == null ? null : v.from(), v == null ? null : v.until(), p.isRevoked());
        }
    }
}
