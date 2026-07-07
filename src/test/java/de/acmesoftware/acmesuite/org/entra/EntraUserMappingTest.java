package de.acmesoftware.acmesuite.org.entra;

import de.acmesoftware.acmesuite.org.domain.Person;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Person -&gt; Graph user payload: complete attributes, no mailbox, stable update payload. */
class EntraUserMappingTest {

    @Test
    void createBodyMapsAttributesWithoutMailbox() {
        Person p = new Person("u-gf-1", "Julia", "Jefa", "j.jefa@acme-group.io", "Geschäftsführerin", null);

        Map<String, Object> b = EntraProvisioner.createBody(p);

        assertThat(b).containsEntry("userPrincipalName", "j.jefa@acme-group.io")
                .containsEntry("mailNickname", "j.jefa")
                .containsEntry("displayName", "Julia Jefa")
                .containsEntry("givenName", "Julia")
                .containsEntry("surname", "Jefa")
                .containsEntry("employeeId", "gf-1")   // u- prefix stripped (Entra limit 16 characters)
                .containsEntry("jobTitle", "Geschäftsführerin")
                .containsEntry("accountEnabled", true);
        assertThat(b).doesNotContainKey("mail");        // no mailbox
        assertThat(b).doesNotContainKey("department");  // no primary OrgUnit set

        @SuppressWarnings("unchecked")
        Map<String, Object> pw = (Map<String, Object>) b.get("passwordProfile");
        assertThat(pw).containsEntry("forceChangePasswordNextSignIn", false);
        assertThat((String) pw.get("password")).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void updateBodyOmitsUpnMailNicknamePassword() {
        Person p = new Person("u-gf-1", "Julia", "Jefa", "j.jefa@acme-group.io", "CFO", null);

        Map<String, Object> b = EntraProvisioner.updateBody(p);

        assertThat(b).containsEntry("displayName", "Julia Jefa").containsEntry("jobTitle", "CFO");
        assertThat(b).doesNotContainKey("userPrincipalName")
                .doesNotContainKey("mailNickname")
                .doesNotContainKey("passwordProfile");
    }
}
