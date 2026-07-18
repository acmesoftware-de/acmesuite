package de.acmesoftware.acmesuite.org.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Employee of the ACME Group.
 *
 * <p>Carries the <b>curated overlay</b> of the canonical org catalog (cf. the reference company
 * catalog, SoR split ADR-016): the reporting line ({@link #manager}), the
 * delegation ({@link #delegateIds}), the assistants ({@link #assistantIds}), and any
 * matrix/secondary memberships ({@link #secondaryUnitIds}). These fields are also the
 * "parameterized rules" of the later 3D agents.
 */
@Entity
@Table(name = "person")
@Audited
@SQLRestriction("deleted_at is null")
public class Person extends AuditedEntity {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", length = 160)
    private String email;

    /** Job title (overlay {@code title}), e.g. "Geschäftsführerin", "CFO (Prokurist)". */
    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Applicant (not yet hired) — does not count as an employee (headcount/payroll). */
    @Column(name = "applicant", nullable = false)
    private boolean applicant = false;

    /** Re-hirable only from this day onward (30-day block after rejection); -1 = no block. */
    @Column(name = "hire_blocked_until_day", nullable = false)
    private long hireBlockedUntilDay = -1;

    /** Recruiting pipeline stage — only meaningful while {@link #applicant} is true; else {@code null}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_stage", length = 16)
    private ApplicantStage applicantStage;

    /** Fit score 0–100 from screening; {@code null} until scored (e.g. still {@code NEW}). */
    @Column(name = "match_score")
    private Integer matchScore;

    /** Day the application was received; {@code null} for long-standing employees. */
    @Column(name = "applied_on")
    private java.time.LocalDate appliedOn;

    /** Compensation type (hourly wage vs. salary) — switchable in ACMEhr. */
    @Enumerated(EnumType.STRING)
    @Column(name = "comp_type", nullable = false, length = 16)
    private CompensationType compType = CompensationType.HOURLY;

    /** Hourly rate in EUR. For {@code SALARIED} the basis of the 9-hour flat rate, for {@code HOURLY} per actual hour. */
    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal hourlyRate = new java.math.BigDecimal("25.00");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_org_unit_id")
    private OrgUnit primaryOrgUnit;

    /** Reporting line (managerKey). Self-reference to the supervising person. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Person manager;

    @NotAudited
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "person_delegate", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "delegate_id", length = 32)
    private Set<String> delegateIds = new LinkedHashSet<>();

    @NotAudited
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "person_assistant", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "assistant_id", length = 32)
    private Set<String> assistantIds = new LinkedHashSet<>();

    @NotAudited
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "person_secondary_unit", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "unit_id", length = 96)
    private Set<String> secondaryUnitIds = new LinkedHashSet<>();

    /**
     * Entra object id (oid), as soon as the person has been provisioned into Azure Entra; {@code null} before.
     * Written back by the Entra provisioner after the Graph upsert and is the anchor for the
     * later SSO subject matching (token {@code sub} = Entra {@code oid}).
     */
    @Column(name = "entra_object_id", length = 64)
    private String entraObjectId;

    protected Person() {
    }

    public Person(String id, String firstName, String lastName, String email, String jobTitle,
                  OrgUnit primaryOrgUnit) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.jobTitle = jobTitle;
        this.primaryOrgUnit = primaryOrgUnit;
    }

    // Overlay is set in a second pass (managers/delegates must exist first).

    public void assignManager(Person manager) {
        this.manager = manager;
    }

    public void updateJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isApplicant() {
        return applicant;
    }

    public void setApplicant(boolean applicant) {
        this.applicant = applicant;
    }

    public long getHireBlockedUntilDay() {
        return hireBlockedUntilDay;
    }

    public void setHireBlockedUntilDay(long day) {
        this.hireBlockedUntilDay = day;
    }

    public ApplicantStage getApplicantStage() {
        return applicantStage;
    }

    public void setApplicantStage(ApplicantStage stage) {
        this.applicantStage = stage;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public java.time.LocalDate getAppliedOn() {
        return appliedOn;
    }

    public void setAppliedOn(java.time.LocalDate appliedOn) {
        this.appliedOn = appliedOn;
    }

    /** Set the recruiting overlay for an applicant in one call (seeding / applicant creation). */
    public void setRecruiting(ApplicantStage stage, Integer matchScore, java.time.LocalDate appliedOn) {
        this.applicantStage = stage;
        this.matchScore = matchScore;
        this.appliedOn = appliedOn;
    }

    public void replaceDelegates(java.util.Collection<String> ids) {
        this.delegateIds.clear();
        this.delegateIds.addAll(ids);
    }

    public void replaceAssistants(java.util.Collection<String> ids) {
        this.assistantIds.clear();
        this.assistantIds.addAll(ids);
    }

    public void addDelegate(String delegatePersonId) {
        this.delegateIds.add(delegatePersonId);
    }

    public void addAssistant(String assistantPersonId) {
        this.assistantIds.add(assistantPersonId);
    }

    public void addSecondaryUnit(String unitId) {
        this.secondaryUnitIds.add(unitId);
    }

    /** Set compensation (switch hourly wage ↔ salary or adjust the hourly rate). */
    public void setCompensation(CompensationType compType, java.math.BigDecimal hourlyRate) {
        this.compType = compType;
        this.hourlyRate = hourlyRate;
    }

    public CompensationType getCompType() {
        return compType;
    }

    public java.math.BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public boolean isActive() {
        return active;
    }

    public OrgUnit getPrimaryOrgUnit() {
        return primaryOrgUnit;
    }

    public Person getManager() {
        return manager;
    }

    public Set<String> getDelegateIds() {
        return delegateIds;
    }

    public Set<String> getAssistantIds() {
        return assistantIds;
    }

    public Set<String> getSecondaryUnitIds() {
        return secondaryUnitIds;
    }

    public String getEntraObjectId() {
        return entraObjectId;
    }

    /** Sets/updates the Entra object id after provisioning (HR -> Entra). */
    public void assignEntraObjectId(String entraObjectId) {
        this.entraObjectId = entraObjectId;
    }
}
