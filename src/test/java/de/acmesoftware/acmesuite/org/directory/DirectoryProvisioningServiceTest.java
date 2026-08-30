package de.acmesoftware.acmesuite.org.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.org.domain.RoleAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * The orchestration decisions of ADR-0011, made testable with a mocked port: who is provisioned,
 * who is disabled, who is left out, and that the anchor is written back only when it changed.
 */
class DirectoryProvisioningServiceTest {

    private final PersonRepository persons = Mockito.mock(PersonRepository.class);
    private final RoleAssignmentRepository roles = Mockito.mock(RoleAssignmentRepository.class);
    private final DirectoryProvisioner directory = Mockito.mock(DirectoryProvisioner.class);

    private static DirectoryProperties props(boolean dryRun, Map<String, String> groups) {
        return new DirectoryProperties(true, "t", "c", "s", "acme-group.io", dryRun, groups);
    }

    private static Person person(String id, String email, boolean active, boolean applicant) {
        Person p = new Person(id, "Vor", "Nach", email, "Titel", null);
        p.setActive(active);
        p.setApplicant(applicant);
        return p;
    }

    private DirectoryProvisioningService service(boolean dryRun, Map<String, String> groups) {
        when(roles.findByPerson_Id(anyString())).thenReturn(List.of());
        return new DirectoryProvisioningService(persons, roles, directory, props(dryRun, groups));
    }

    @Test
    void applicantsAreLeftOutAndCountedSeparately() {
        when(persons.findAll()).thenReturn(List.of(
                person("u-1", "a@acme-group.io", true, false),
                person("u-2", "b@acme-group.io", true, true)));   // applicant
        when(directory.findObjectId(anyString())).thenReturn(Optional.empty());
        when(directory.create(any(), anyString())).thenReturn("oid-new");

        ProvisioningRun run = service(false, Map.of()).provision();

        assertThat(run.eligible()).isEqualTo(1);
        assertThat(run.skippedApplicants()).isEqualTo(1);
        // The applicant is never even looked up in the directory.
        verify(directory, never()).findObjectId("b@acme-group.io");
        verify(directory, times(1)).create(any(), anyString());
    }

    @Test
    void aLeaverWithAnAccountIsDisabledNotUpdated() {
        Person left = person("u-3", "c@acme-group.io", false, false);
        left.assignDirectoryObjectId("oid-3");
        when(persons.findAll()).thenReturn(List.of(left));

        ProvisioningRun run = service(false, Map.of()).provision();

        assertThat(run.disabled()).isEqualTo(1);
        verify(directory).disable("oid-3");
        verify(directory, never()).updateAttributes(anyString(), any());
    }

    @Test
    void aLeaverWithoutAnAccountIsNotProvisionedIntoExistence() {
        when(persons.findAll()).thenReturn(List.of(person("u-4", "d@acme-group.io", false, false)));
        when(directory.findObjectId(anyString())).thenReturn(Optional.empty());

        ProvisioningRun run = service(false, Map.of()).provision();

        assertThat(run.created()).isZero();
        assertThat(run.disabled()).isZero();
        verify(directory, never()).create(any(), anyString());
        verify(directory, never()).disable(anyString());
    }

    @Test
    void theMailFlowsThroughToTheDirectory() {
        when(persons.findAll()).thenReturn(List.of(person("u-5", "e.mann@acme-group.io", true, false)));
        when(directory.findObjectId(anyString())).thenReturn(Optional.of("oid-5"));

        service(false, Map.of()).provision();

        ArgumentCaptor<DirectoryPerson> captor = ArgumentCaptor.forClass(DirectoryPerson.class);
        verify(directory).updateAttributes(eq("oid-5"), captor.capture());
        assertThat(captor.getValue().mail()).isEqualTo("e.mann@acme-group.io");
    }

    @Test
    void dryRunWritesNothing() {
        when(persons.findAll()).thenReturn(List.of(person("u-6", "f@acme-group.io", true, false)));

        ProvisioningRun run = service(true, Map.of()).provision();

        assertThat(run.dryRun()).isTrue();
        assertThat(run.eligible()).isEqualTo(1);
        verify(directory, never()).findObjectId(anyString());
        verify(directory, never()).create(any(), anyString());
        verify(directory, never()).updateAttributes(anyString(), any());
    }

    @Test
    void anEmptyGroupMappingTouchesNoGroup() {
        Person p = person("u-7", "g@acme-group.io", true, false);
        p.assignDirectoryObjectId("oid-7");
        when(persons.findAll()).thenReturn(List.of(p));

        service(false, Map.of()).provision();

        verify(directory, never()).addToGroup(anyString(), anyString());
    }
}
