package de.acmesoftware.acmesuite.org.entra;

import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Dry run only plans, does not call Graph and writes nothing; applicants are skipped. */
class EntraProvisionerDryRunTest {

    @Test
    void dryRunPlansWithoutGraphAndWithoutSave() {
        PersonRepository repo = mock(PersonRepository.class);
        EntraGraphClient graph = mock(EntraGraphClient.class);

        Person aktiv1 = new Person("u-gf-1", "Julia", "Jefa", "j.jefa@acme-group.io", "Geschäftsführerin", null);
        Person aktiv2 = new Person("u-finance-cfo", "Friedrich", "Finance", "f.finance@acme-group.io", "CFO", null);
        Person bewerber = new Person("u-app", "Anton", "Applicant", "a.applicant@acme-group.io", "Teammitglied", null);
        bewerber.setApplicant(true);
        when(repo.findAll()).thenReturn(List.of(aktiv1, aktiv2, bewerber));

        EntraProperties props = new EntraProperties(true, "tid", "cid", "secret", "acme-group.io", true);
        ProvisionSummary s = new EntraProvisioner(repo, graph, props).provision();

        assertThat(s.dryRun()).isTrue();
        assertThat(s.eligible()).isEqualTo(2);
        assertThat(s.skippedApplicants()).isEqualTo(1);
        assertThat(s.created()).isZero();
        assertThat(s.updated()).isZero();
        verifyNoInteractions(graph);
        verify(repo, never()).saveAll(any());
    }
}
