package de.acmesoftware.acmesuite.process;

import de.acmesoftware.acmesuite.org.OrgDirectory;
import de.acmesoftware.acmesuite.org.OrgViews.PersonView;
import de.acmesoftware.acmesuite.org.OrgViews.PowerOfAttorneyView;
import de.acmesoftware.acmesuite.shared.ContractApproved;
import de.acmesoftware.acmesuite.shared.CountryPolicy;
import de.acmesoftware.acmesuite.shared.CountryRestriction;
import de.acmesoftware.acmesuite.shared.ManualApprovalRequested;
import de.acmesoftware.acmesuite.shared.Rng;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Process engine of "ACME offline": generates the daily workload from
 * purchasing/sales/HR (seed-deterministic), resolves the approvers via {@link ApprovalRouting} + {@link OrgDirectory}
 * and drives each contract through the manual circulation as a state machine. The choreography (which
 * assistant carries which folder where and when) drives the workflow; only the logic lives here.
 */
@Component
public class ProcessEngine {

    private static final List<String> CARRIERS = List.of("u-einkauf-asst", "u-vertrieb-asst", "u-hr-asst");
    private static final Material[] MATERIALS = Material.values();
    private static final Product[] PRODUCTS = Product.values();
    /** Fallback countries in case the CountryPolicy is (not yet) available (e.g. unit test without adapter). */
    private static final List<String> DEFAULT_COUNTRIES = List.of("Acmerica", "Widgeton", "Fidgetia", "Desertia");

    // Duration of the email/intake stages in ticks (envelope on the PC).
    private static final int STAGE_FACH = 8;
    private static final int STAGE_EINKAUF = 8;
    private static final int STAGE_SALES = 12;
    /** Backlog cap: from this many open transactions (contracts + intakes) on, no new ones are accepted. */
    private static final int BACKLOG_CAP = 400;
    // If a sales intake waits in vain this long for a free meeting room, the deal is closed
    // "by phone/email" → the sales contract is still created (no starvation of the sales side).
    private static final int MEETING_WAIT_MAX = 80;

    /**
     * Pre-contract stage: the incoming "envelope". Purchasing: first at the specialist department, then at
     * purchasing; sales: blinks at the sales team. When the stage runs out, the contract is created.
     */
    private static final class Intake {
        String id;
        ContractType type;
        String unitKey;
        String carrierKey;
        String subject;
        List<ApprovalStep> steps;
        String pcPersonKey;   // whose PC currently shows the envelope
        String forwardToKey;  // purchasing: next station
        boolean blink;        // sales: icon blinks
        int remaining;        // remaining ticks of the current stage
        int stage;            // 0 = first station, 1 = purchasing
        boolean awaitingMeeting; // sales: waiting for the sales meeting
        boolean meetingStarted;  // sales: meeting has been convened
        int meetingWait = MEETING_WAIT_MAX; // remaining patience until the deal is closed without a meeting
        long valueEur;
        boolean restricted;
        List<String> restrictionLabels = List.of();
    }

    private final OrgDirectory org;
    private final ApprovalRouting routing;
    private final ApplicationEventPublisher events;
    private final long seed;
    /** Maintainable country restrictions (optionally injected; without an adapter the fallback applies). */
    private CountryPolicy countryPolicy;

    private final List<Contract> active = new ArrayList<>();
    private final java.util.Deque<ArchivedContract> archive = new java.util.ArrayDeque<>();
    private static final int ARCHIVE_CAP = 300; // most recent N completed/rejected transactions
    private final List<Intake> intakes = new ArrayList<>();
    private int created;
    private int directBuys;
    private int onlineOrders;
    private int completed;
    private int rejected;
    private int seq;
    private int intakeSeq;
    private long currentDay; // last day processed (for the 30-day lockout after an applicant rejection)

    public ProcessEngine(OrgDirectory org, ApprovalRouting routing, ApplicationEventPublisher events,
                         @Value("${acme.sim.seed:42}") long seed) {
        this.org = org;
        this.routing = routing;
        this.events = events;
        this.seed = seed;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setCountryPolicy(CountryPolicy countryPolicy) {
        this.countryPolicy = countryPolicy;
    }

    private List<String> countryPool(boolean sourcing) {
        if (countryPolicy != null) {
            List<String> pool = sourcing ? countryPolicy.sourcingCountries() : countryPolicy.marketCountries();
            if (!pool.isEmpty()) {
                return pool;
            }
        }
        return DEFAULT_COUNTRIES;
    }

    private CountryRestriction restrictionOf(String country) {
        return countryPolicy != null ? countryPolicy.restrictionFor(country) : CountryRestriction.NONE;
    }

    /**
     * ACME analog: an external transaction (e.g. ACMEcrm order above the threshold) needs a
     * signature → creates a sales folder that is carried through the hustle and bustle to the authorized signatory
     * (smallest sufficient approval limit, via ACMEhr) and signed.
     */
    @EventListener
    public synchronized void onManualApprovalRequested(ManualApprovalRequested e) {
        if ("HIRE".equals(e.kind())) {
            // Hiring an applicant: contract HR officer → HR management → executive management.
            List<String> approvers = new ArrayList<>();
            for (ApprovalStep s : routing.forHr(ContractType.HR_HIRE)) {
                String key = resolve(s);
                if (key != null) {
                    approvers.add(key);
                }
            }
            if (!approvers.isEmpty()) {
                Contract c = new Contract("C-" + (++seq), ContractType.HR_HIRE, e.subject(),
                        "ou-hr", "u-hr-asst", approvers, e.ref());
                c.setApprovalContext(0, false, List.of());
                active.add(c);
                created++;
            }
            return;
        }
        String approver = smallestSufficientSignatory(BigDecimal.valueOf(e.valueEur()));
        boolean purchase = "PURCHASE".equals(e.kind());
        ContractType type = purchase ? ContractType.PURCHASE : ContractType.SALES;
        String unit = purchase ? "ou-einkauf" : "ou-vertrieb";
        String carrier = purchase ? "u-einkauf-asst" : "u-vertrieb-asst";
        Contract c = new Contract("C-" + (++seq), type, e.subject(), unit, carrier, List.of(approver), e.ref());
        c.setApprovalContext(e.valueEur(), false, List.of());
        active.add(c);
        created++;
    }

    /** Who can sign {@code value} on the reference date — with the smallest sufficient limit? Fallback executive management. */
    private String smallestSufficientSignatory(BigDecimal value) {
        return org.signatoriesFor("acme", value, "EUR", LocalDate.now()).stream()
                .min(Comparator.comparing((PowerOfAttorneyView p) -> p.unlimited() ? BigDecimal.valueOf(Long.MAX_VALUE)
                        : p.limitAmount()))
                .map(PowerOfAttorneyView::holderId)
                .orElse("u-gf-1");
    }

    /**
     * Daily inflow. Purchasing/sales start as an <b>email intake</b> (envelope on the PC), which
     * only becomes a contract after the email stages. Auxiliary-material direct purchases and standard online
     * orders need no contract (only counted). HR termination: rare, directly from executive management.
     */
    public synchronized void spawnDailyWork(long day) {
        currentDay = day;
        // Trigger autonomous daily demand (CRM customer orders, material resupply) → real orders
        // that lead to production/revenue via approval (the rest here is the visible folder circulation).
        events.publishEvent(new de.acmesoftware.acmesuite.shared.SimDayStarted(day));
        // Backlog limiting (back-pressure): if the folder pipeline is overloaded, the office accepts no
        // MORE new transactions until the backlog is worked off. Prevents unbounded growth of
        // active/intakes (otherwise the backlog eventually eats up memory → OOM, processing stalls).
        if (active.size() + intakes.size() >= BACKLOG_CAP) {
            return;
        }
        for (int i = 0; i < 2; i++) { // purchasing 2/day (sales 3/day) — inflow skewed toward sales
            Material m = MATERIALS[Rng.intIn(seed, "pm", day, i, MATERIALS.length)];
            long value = 2_000 + (long) (Rng.unit(seed, "pv", day, i) * 300_000);
            List<String> pool = countryPool(true);
            String source = pool.get(Rng.intIn(seed, "pc", day, i, pool.size()));
            Restrictions r = Restrictions.forPurchase(m, restrictionOf(source));
            boolean spotCheck = Rng.unit(seed, "pck", day, i) < 0.02;
            var steps = routing.forPurchase(m, value, r, spotCheck);
            if (steps.isEmpty()) {
                directBuys++; // easily available auxiliary material → direct purchase
                continue;
            }
            // Purchasing email: specialist department of the product line → purchasing.
            String fachLead = org.leadOf(m == Material.DEMOKRATIUM ? "ou-fb-a" : "ou-fb-b")
                    .map(PersonView::id).orElse("u-einkauf-1");
            Intake in = new Intake();
            in.id = "IN-" + (++intakeSeq);
            in.type = ContractType.PURCHASE;
            in.unitKey = "ou-einkauf";
            in.carrierKey = "u-einkauf-asst";
            in.subject = subject(m.displayName() + " " + (value / 1000) + "k€", r);
            in.steps = steps;
            in.valueEur = value;
            in.restricted = r.any();
            in.restrictionLabels = r.labels();
            in.pcPersonKey = fachLead;
            in.forwardToKey = "u-einkauf-1";
            in.remaining = STAGE_FACH;
            in.stage = 0;
            intakes.add(in);
        }
        for (int i = 0; i < 3; i++) {
            Product p = PRODUCTS[Rng.intIn(seed, "sp", day, i, PRODUCTS.length)];
            List<String> pool = countryPool(false);
            String country = pool.get(Rng.intIn(seed, "sc", day, i, pool.size()));
            int qty = 100 + Rng.intIn(seed, "sq", day, i, 9900);
            long value = 2_000 + (long) (Rng.unit(seed, "sv", day, i) * 300_000);
            boolean byAir = Rng.unit(seed, "sa", day, i) < 0.15;
            Restrictions r = Restrictions.forSales(p, restrictionOf(country), byAir);
            boolean spotCheck = Rng.unit(seed, "sck", day, i) < 0.02;
            var steps = routing.forSales(p, value, r, spotCheck);
            if (steps.isEmpty()) {
                onlineOrders++; // standard online order → no contract
                continue;
            }
            // Sales email: blinks at the sales team; after the email stage a real meeting follows.
            Intake in = new Intake();
            in.id = "IN-" + (++intakeSeq);
            in.type = ContractType.SALES;
            in.unitKey = "ou-vertrieb";
            in.carrierKey = "u-vertrieb-asst";
            in.subject = subject(p.displayName() + " → " + country + " ×" + qty, r);
            in.steps = steps;
            in.valueEur = value;
            in.restricted = r.any();
            in.restrictionLabels = r.labels();
            in.pcPersonKey = "u-vertrieb-1";
            in.blink = true;
            in.remaining = STAGE_SALES;
            in.stage = 0;
            intakes.add(in);
        }
        if (Rng.unit(seed, "hr", day, 0) < 0.02) {
            addContract(ContractType.HR_TERMINATION, "ou-hr", "u-hr-asst",
                    ContractType.HR_TERMINATION.displayName(), routing.forHr(ContractType.HR_TERMINATION),
                    0, false, List.of());
        }
    }

    /** Subject with appended restriction labels (e.g. "Despotium 120k€ [Sanction·CO₂]"). */
    private static String subject(String base, Restrictions r) {
        return r.any() ? base + " [" + String.join("·", r.labels()) + "]" : base;
    }

    /**
     * Advances the email intakes by one tick. Purchasing: specialist department → purchasing → contract.
     * Sales: after the email stage NOT directly to a contract, but {@code awaitingMeeting} — a
     * sales meeting is convened; only its end creates the contract
     * ({@link #completeSalesMeeting}).
     */
    public synchronized void tickIntakes() {
        var done = new ArrayList<Intake>();
        for (Intake in : intakes) {
            if (in.awaitingMeeting) {
                // As long as no meeting has been convened (no room free), patience dwindles;
                // when it runs out, the deal is closed without a meeting → sales contract is created.
                if (!in.meetingStarted && --in.meetingWait <= 0) {
                    addContract(in.type, in.unitKey, in.carrierKey, in.subject, in.steps,
                            in.valueEur, in.restricted, in.restrictionLabels);
                    done.add(in);
                }
                continue; // otherwise: waits for the meeting to be convened
            }
            if (--in.remaining > 0) {
                continue;
            }
            if (in.type == ContractType.PURCHASE && in.stage == 0) {
                in.stage = 1; // on to purchasing
                in.pcPersonKey = in.forwardToKey;
                in.remaining = STAGE_EINKAUF;
            } else if (in.type == ContractType.SALES) {
                in.awaitingMeeting = true; // email read → a meeting is needed
                in.blink = false;
                in.pcPersonKey = null;     // envelope disappears
            } else {
                addContract(in.type, in.unitKey, in.carrierKey, in.subject, in.steps,
                        in.valueEur, in.restricted, in.restrictionLabels);
                done.add(in);
            }
        }
        intakes.removeAll(done);
    }

    /** Sales intakes waiting for a not-yet-convened meeting (IDs). */
    public synchronized List<String> salesMeetingRequests() {
        List<String> r = new ArrayList<>();
        for (Intake in : intakes) {
            if (in.awaitingMeeting && !in.meetingStarted) {
                r.add(in.id);
            }
        }
        return r;
    }

    /** Marks that a meeting is running for this sales intake (no re-convening). */
    public synchronized void markSalesMeetingStarted(String intakeId) {
        for (Intake in : intakes) {
            if (in.id.equals(intakeId)) {
                in.meetingStarted = true;
                return;
            }
        }
    }

    /** Meeting ended → the sales intake now becomes the sales contract. */
    public synchronized void completeSalesMeeting(String intakeId) {
        Intake found = null;
        for (Intake in : intakes) {
            if (in.id.equals(intakeId)) {
                found = in;
                break;
            }
        }
        if (found != null) {
            intakes.remove(found);
            addContract(found.type, found.unitKey, found.carrierKey, found.subject, found.steps,
                    found.valueEur, found.restricted, found.restrictionLabels);
        }
    }

    /** Persons with an envelope on the PC → blink (sales) or steady (purchasing email). */
    public synchronized Map<String, Boolean> mailPersons() {
        Map<String, Boolean> m = new HashMap<>();
        for (Intake in : intakes) {
            if (in.pcPersonKey != null) {
                m.put(in.pcPersonKey, in.blink);
            }
        }
        return m;
    }

    private void addContract(ContractType type, String unitKey, String carrierKey, String subject,
                             List<ApprovalStep> steps, long valueEur, boolean restricted,
                             List<String> restrictionLabels) {
        List<String> approvers = new ArrayList<>();
        for (ApprovalStep s : steps) {
            String key = resolve(s);
            if (key != null) {
                approvers.add(key);
            }
        }
        if (approvers.isEmpty()) {
            completed++;
            return;
        }
        Contract c = new Contract("C-" + (++seq), type, subject, unitKey, carrierKey, approvers);
        c.setApprovalContext(valueEur, restricted, restrictionLabels);
        active.add(c);
        created++;
    }

    private String resolve(ApprovalStep s) {
        if (s.fixedPersonKey() != null) {
            return s.fixedPersonKey();
        }
        return org.leadOf(s.unitKey()).map(PersonView::id).orElse(null);
    }

    // ---- choreography driven by the daily cycle ----

    public synchronized List<String> carrierKeys() {
        return CARRIERS;
    }

    /** Pick up this assistant's next waiting folder (→ CIRCULATING); null if nothing is pending. */
    public synchronized String nextPendingFor(String carrierKey) {
        for (Contract c : active) {
            if (c.carrierKey().equals(carrierKey) && c.status() == Contract.Status.PENDING) {
                c.markCirculating();
                return c.id();
            }
        }
        return null;
    }

    public synchronized Contract byId(String id) {
        for (Contract c : active) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /** Current station signed → continue; true if the circulation is thereby finished. */
    public synchronized boolean signCurrentStep(String id) {
        Contract c = byId(id);
        if (c == null) {
            return true;
        }
        boolean done = c.signCurrentStep();
        if (done) {
            active.remove(c);
            completed++;
            archive(c, "COMPLETED");
            if (c.externalRef() != null) {
                events.publishEvent(new ContractApproved(c.externalRef(), c.lastApproverKey()));
            }
        }
        return done;
    }

    // ---- Folder realism (Slice 5): inbox per station, reading time, error→rework ----

    /** Rejection from the second step on (the normal case, → transaction stopped). */
    private static final double REJECT_PROB = 0.01;
    /** Rework to the previous position (with justification). */
    private static final double REWORK_PROB = 0.02;
    /** For restrictions, the respective executive management rejects with 20 %. */
    private static final double GF_RESTRICTED_REJECT_PROB = 0.20;
    /** Hiring an applicant: 5 % rejection per step. */
    private static final double HIRE_REJECT_PROB = 0.05;
    /** Share of rework cases whose justification is "price" (otherwise it only costs time). */
    private static final double PRICE_REASON_PROB = 0.5;
    /** How many times the same folder may go into rework at most; after that it is waved through. */
    private static final int MAX_REWORK = 2;

    /** Result of a completed review: rework, rejection or signature. */
    public enum ReadOutcome { REWORK, REJECTED, SIGNED_MORE, SIGNED_DONE }

    /** This assistant's next folder to pick up (phase WAITING) → PREP. Null if nothing is pending. */
    public synchronized String nextWaitingFor(String carrierKey) {
        for (Contract c : active) {
            if (c.carrierKey().equals(carrierKey) && c.stage() == Contract.Stage.WAITING) {
                c.markCirculating();
                c.setStage(Contract.Stage.PREP);
                return c.id();
            }
        }
        return null;
    }

    public synchronized boolean isFirstLeg(String id) {
        Contract c = byId(id);
        return c != null && c.firstLeg();
    }

    public synchronized void setStage(String id, Contract.Stage stage) {
        Contract c = byId(id);
        if (c != null) {
            c.setStage(stage);
        }
    }

    /**
     * Reading/reviewing at the station finished: with probability {@value #READ_ERROR_PROB} the
     * station finds an error → folder back into the inbox (rework, up to {@value #MAX_REWORK}×). Otherwise
     * signature: next station (SIGNED_MORE) or circulation done (SIGNED_DONE, callback to CRM/Supply).
     */
    public synchronized ReadOutcome finishReading(String id) {
        Contract c = byId(id);
        if (c == null) {
            return ReadOutcome.SIGNED_DONE;
        }
        int attempt = c.nextAttempt();
        String approver = c.currentApproverKey();
        boolean gfStep = approver != null && approver.startsWith("u-gf");
        boolean purchase = c.type() == ContractType.PURCHASE;
        double roll = Rng.unit(seed, id, attempt, 71);

        boolean hire = c.type() == ContractType.HR_HIRE;
        // From the second step on (index ≥ 1): rejection / rework possible. Hiring: 5 % per step.
        if (c.step() >= 1) {
            double rejectProb = hire ? HIRE_REJECT_PROB
                    : (gfStep && c.restricted()) ? GF_RESTRICTED_REJECT_PROB : REJECT_PROB;
            if (roll < rejectProb) {
                c.setReason(gfStep && c.restricted() ? "abgelehnt (Restriktion)" : "abgelehnt");
                return drop(c, ReadOutcome.REJECTED);
            }
            if (!hire && c.reworkCount() < MAX_REWORK && roll < rejectProb + REWORK_PROB) {
                boolean priceReason = Rng.unit(seed, id, attempt, 72) < PRICE_REASON_PROB;
                if (priceReason) {
                    boolean concession = Rng.unit(seed, id, attempt, 73) < 0.5;
                    if (!concession) {
                        // Price justification, no agreement → cancellation by supplier/customer.
                        c.setReason(purchase ? "Cancelled by supplier" : "Cancelled by customer");
                        return drop(c, ReadOutcome.REJECTED);
                    }
                    // Agreement: lower (purchasing) or higher (sales) price → back one position.
                    long delta = Math.max(1, c.valueEur() / 10);
                    c.setValueEur(Math.max(0, purchase ? c.valueEur() - delta : c.valueEur() + delta));
                    c.setReason(purchase ? "Discount" : "Surcharge");
                } else {
                    c.setReason("Rework"); // other justification → costs time
                }
                c.addRework();
                c.stepBack();
                c.setStage(Contract.Stage.WAITING); // the carrier brings it back to the previous position
                return ReadOutcome.REWORK;
            }
        }
        boolean done = c.signCurrentStep();
        if (done) {
            active.remove(c);
            completed++;
            archive(c, "COMPLETED");
            if (c.externalRef() != null) {
                events.publishEvent(new ContractApproved(c.externalRef(), c.lastApproverKey()));
            }
            return ReadOutcome.SIGNED_DONE;
        }
        c.setStage(Contract.Stage.WAITING); // the next station needs a carrier again
        return ReadOutcome.SIGNED_MORE;
    }

    /** Remove a transaction from circulation (rejection/cancellation). */
    private ReadOutcome drop(Contract c, ReadOutcome outcome) {
        active.remove(c);
        rejected++;
        archive(c, "REJECTED");
        if (c.externalRef() != null) {
            events.publishEvent(new de.acmesoftware.acmesuite.shared.ContractRejected(c.externalRef(), currentDay));
        }
        return outcome;
    }

    /** Place a completed/rejected transaction into the (capped) archive — most recent first. */
    private void archive(Contract c, String outcome) {
        archive.addFirst(new ArchivedContract(ContractView.of(c), outcome, currentDay));
        while (archive.size() > ARCHIVE_CAP) {
            archive.removeLast();
        }
    }

    // ---- Read views ----

    /** Reset: discard all folders, email intakes and counters (fresh start). */
    public synchronized void reset() {
        active.clear();
        archive.clear();
        intakes.clear();
        created = 0;
        directBuys = 0;
        onlineOrders = 0;
        completed = 0;
        rejected = 0;
        seq = 0;
        intakeSeq = 0;
    }

    public synchronized List<ContractView> activeContracts() {
        return active.stream().map(ContractView::of).toList();
    }

    /** Archive of completed/rejected transactions, most recent first. */
    public synchronized List<ArchivedContract> archivedContracts() {
        return List.copyOf(archive);
    }

    /** Archive entry: contract view + outcome (COMPLETED/REJECTED) + day. */
    public record ArchivedContract(ContractView contract, String outcome, long day) {
    }

    public synchronized ProcessStats stats() {
        int inbox = 0;
        int reading = 0;
        int rework = 0;
        for (Contract c : active) {
            if (c.stage() == Contract.Stage.INBOX) {
                inbox++;
            } else if (c.stage() == Contract.Stage.READING) {
                reading++;
            }
            if (c.reworkCount() > 0) {
                rework++;
            }
        }
        return new ProcessStats(created, directBuys, onlineOrders, completed, rejected, active.size(),
                inbox, reading, rework);
    }

    public record ProcessStats(int created, int directBuys, int onlineOrders, int completed, int rejected,
                               int active, int inbox, int reading, int rework) {
    }
}
