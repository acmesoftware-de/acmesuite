package de.acmesoftware.acmesuite.org.domain;

import de.acmesoftware.acmesuite.shared.DateRange;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Time-limited absence of a person with a named substitute. Controls who takes over the
 * work (the substitute steps in along the reporting line / on tasks).
 */
@Entity
@Table(name = "absence")
public class Absence {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** Business key/reason, e.g. "urlaub-1", "kur-1". */
    @Column(name = "reason_key", length = 64, nullable = false)
    private String reasonKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private AbsenceType type = AbsenceType.VACATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AbsenceStatus status = AbsenceStatus.APPROVED;

    @Column(name = "note", length = 512)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_id")
    private Person substitute;

    @Embedded
    private DateRange period;

    protected Absence() {
    }

    /** Legacy/seed constructor: derives the type from the reasonKey, status = approved. */
    public Absence(String id, Person person, String reasonKey, Person substitute, DateRange period) {
        this(id, person, reasonKey,
                reasonKey != null && reasonKey.startsWith("kur") ? AbsenceType.CURE : AbsenceType.VACATION,
                AbsenceStatus.APPROVED, substitute, period, null);
    }

    public Absence(String id, Person person, String reasonKey, AbsenceType type, AbsenceStatus status,
                   Person substitute, DateRange period, String note) {
        this.id = id;
        this.person = person;
        this.reasonKey = reasonKey;
        this.type = type;
        this.status = status;
        this.substitute = substitute;
        this.period = period;
        this.note = note;
    }

    public void changeStatus(AbsenceStatus status) {
        this.status = status;
    }

    public void reschedule(DateRange period) {
        this.period = period;
    }

    public void assignSubstitute(Person substitute) {
        this.substitute = substitute;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean coversDate(LocalDate date) {
        return period != null && period.isActiveOn(date);
    }

    public String getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public String getReasonKey() {
        return reasonKey;
    }

    public AbsenceType getType() {
        return type;
    }

    public AbsenceStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public Person getSubstitute() {
        return substitute;
    }

    public DateRange getPeriod() {
        return period;
    }
}
