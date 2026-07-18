package de.acmesoftware.acmesuite.process;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.org.OrgDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration: the process engine creates email intakes from the daily work, from those—after the
 * email stages—contracts, resolves the approvers against the real org and runs a circulation
 * through to completion. Autostart off → we drive it ourselves.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@TestPropertySource(properties = {"acme.sim.autostart=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProcessFlowTest {

    @Autowired
    ProcessEngine process;

    @Autowired
    OrgDirectory org;

    /** Tick the intakes long enough until the emails have become contracts. */
    private void drainIntakes() {
        for (int i = 0; i < 30; i++) {
            process.tickIntakes();
        }
    }

    @Test
    void dailyWorkCreatesContractsWithResolvedApprovers() {
        process.spawnDailyWork(1);
        drainIntakes();

        var contracts = process.activeContracts();
        assertThat(contracts).isNotEmpty();
        assertThat(contracts).allSatisfy(c -> {
            assertThat(c.currentApproverKey()).isNotNull();
            // The approver is a really existing person in the org.
            assertThat(org.person(c.currentApproverKey())).isPresent();
            assertThat(c.totalSteps()).isPositive();
        });
    }

    @Test
    void salesContractAppearsOnlyAfterTheMeeting() {
        // Spawn over several days until a sales intake is waiting for its meeting.
        java.util.List<String> requests = java.util.List.of();
        for (long day = 1; day <= 40 && requests.isEmpty(); day++) {
            process.spawnDailyWork(day);
            drainIntakes();
            requests = process.salesMeetingRequests();
        }
        assertThat(requests).as("irgendwann wartet ein Sales-Intake aufs Meeting").isNotEmpty();

        String intakeId = requests.get(0);
        int before = process.activeContracts().size();
        process.markSalesMeetingStarted(intakeId);
        // Without the meeting ending, NO contract is created.
        assertThat(process.activeContracts()).hasSize(before);

        // Meeting ended → now the supply contract is created and the intake is gone.
        process.completeSalesMeeting(intakeId);
        assertThat(process.activeContracts()).hasSize(before + 1);
        assertThat(process.salesMeetingRequests()).doesNotContain(intakeId);
    }

    @Test
    void mappenRealismus_pickupIntoInboxReadThenSign() {
        process.spawnDailyWork(3);
        drainIntakes();
        var c = process.activeContracts().get(0);

        // Assistant picks up the waiting folder → phase PREP.
        String id = process.nextWaitingFor(c.carrierKey());
        assertThat(id).isNotNull();
        assertThat(stageOf(id)).isEqualTo("PREP");

        // Placed in the station's inbox tray → shows up in the tray backlog of the stats.
        process.setStage(id, Contract.Stage.INBOX);
        assertThat(process.stats().inbox()).isGreaterThanOrEqualTo(1);

        // Station starts reading → counts as "under review".
        process.setStage(id, Contract.Stage.READING);
        assertThat(process.stats().reading()).isGreaterThanOrEqualTo(1);

        // Finish reading: error→rework is capped at max. 2×, after that signed.
        ProcessEngine.ReadOutcome out = null;
        for (int i = 0; i < 6 && !signed(out); i++) {
            out = process.finishReading(id);
        }
        assertThat(signed(out)).as("rework is capped → the folder is reliably signed").isTrue();
    }

    private static boolean signed(ProcessEngine.ReadOutcome o) {
        return o == ProcessEngine.ReadOutcome.SIGNED_DONE || o == ProcessEngine.ReadOutcome.SIGNED_MORE;
    }

    private String stageOf(String id) {
        return process.activeContracts().stream().filter(v -> v.id().equals(id)).findFirst().orElseThrow().stage();
    }

    @Test
    void signingEveryStepCompletesACirculation() {
        process.spawnDailyWork(2);
        drainIntakes();
        var first = process.activeContracts().get(0);
        int before = process.stats().completed();

        boolean done = false;
        for (int i = 0; i < first.totalSteps() && !done; i++) {
            done = process.signCurrentStep(first.id());
        }

        assertThat(done).isTrue();
        assertThat(process.byId(first.id())).isNull(); // removed from the active list
        assertThat(process.stats().completed()).isEqualTo(before + 1);
    }
}
