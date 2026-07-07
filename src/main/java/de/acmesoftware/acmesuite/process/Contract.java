package de.acmesoftware.acmesuite.process;

import java.util.List;

/**
 * A contract in the manual circulation of "ACME offline": created in a department, carried step by step
 * in the colored folder by the assistant ({@link #carrierKey}) to the approvers
 * ({@link #approverKeys}), who sign at their desk. Transient (not persisted).
 */
public final class Contract {

    public enum Status { PENDING, CIRCULATING, COMPLETED }

    /**
     * Folder realism: phase in the manual circulation. WAITING = waiting to be picked up by the assistant,
     * PREP = assistant is preparing (printout → folder), CARRYING = being carried to the current station,
     * INBOX = sitting in the station's inbox, READING = being read/reviewed there.
     */
    public enum Stage { WAITING, PREP, CARRYING, INBOX, READING }

    private final String id;
    private final ContractType type;
    private final String subject;
    private final String creatorUnitKey;
    private final String carrierKey;
    private final List<String> approverKeys;
    /** Optional external reference (e.g. ACMEcrm order) that is reported back on completion. */
    private final String externalRef;

    private int step;
    private Status status = Status.PENDING;
    private Stage stage = Stage.WAITING;
    private int reworkCount; // how often a station demanded rework
    private int attempts;    // total reviews read (for deterministic rolls)

    // Transaction context (for routing consequences: rejection/rework/price).
    private long valueEur;
    private boolean restricted;
    private List<String> restrictionLabels = List.of();
    private String reason; // last rework/rejection justification

    public Contract(String id, ContractType type, String subject, String creatorUnitKey,
                    String carrierKey, List<String> approverKeys) {
        this(id, type, subject, creatorUnitKey, carrierKey, approverKeys, null);
    }

    public Contract(String id, ContractType type, String subject, String creatorUnitKey,
                    String carrierKey, List<String> approverKeys, String externalRef) {
        this.id = id;
        this.type = type;
        this.subject = subject;
        this.creatorUnitKey = creatorUnitKey;
        this.carrierKey = carrierKey;
        this.approverKeys = List.copyOf(approverKeys);
        this.externalRef = externalRef;
    }

    public String externalRef() {
        return externalRef;
    }

    /** Last approver actually reached (for the completion callback). */
    public String lastApproverKey() {
        return approverKeys.isEmpty() ? null : approverKeys.get(approverKeys.size() - 1);
    }

    /** Current approver to reach (null when done). */
    public String currentApproverKey() {
        return step < approverKeys.size() ? approverKeys.get(step) : null;
    }

    /** Signature done → next station; true if the circulation is thereby completed. */
    boolean signCurrentStep() {
        step++;
        if (step >= approverKeys.size()) {
            status = Status.COMPLETED;
            return true;
        }
        return false;
    }

    void markCirculating() {
        if (status == Status.PENDING) {
            status = Status.CIRCULATING;
        }
    }

    Stage stage() {
        return stage;
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    public int reworkCount() {
        return reworkCount;
    }

    void addRework() {
        reworkCount++;
    }

    /** Next deterministic roll index (counts every completed review). */
    int nextAttempt() {
        return ++attempts;
    }

    /** Rework: one step back (to the previous station); never below 0. */
    void stepBack() {
        if (step > 0) {
            step--;
        }
    }

    void setApprovalContext(long valueEur, boolean restricted, List<String> labels) {
        this.valueEur = valueEur;
        this.restricted = restricted;
        this.restrictionLabels = List.copyOf(labels);
    }

    public long valueEur() {
        return valueEur;
    }

    void setValueEur(long valueEur) {
        this.valueEur = valueEur;
    }

    public boolean restricted() {
        return restricted;
    }

    public List<String> restrictionLabels() {
        return restrictionLabels;
    }

    public String reason() {
        return reason;
    }

    void setReason(String reason) {
        this.reason = reason;
    }

    /** First station? Then the assistant prepares the folder (printout); later stations do not. */
    boolean firstLeg() {
        return step == 0;
    }

    public String id() {
        return id;
    }

    public ContractType type() {
        return type;
    }

    public FolderColor color() {
        return type.folderColor();
    }

    public String subject() {
        return subject;
    }

    public String creatorUnitKey() {
        return creatorUnitKey;
    }

    public String carrierKey() {
        return carrierKey;
    }

    public List<String> approverKeys() {
        return approverKeys;
    }

    public int step() {
        return step;
    }

    public int totalSteps() {
        return approverKeys.size();
    }

    public Status status() {
        return status;
    }
}
