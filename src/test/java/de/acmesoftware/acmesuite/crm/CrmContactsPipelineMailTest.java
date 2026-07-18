package de.acmesoftware.acmesuite.crm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.acmesoftware.acmesuite.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Contract test of the CRM contacts / pipeline / mail endpoints (v0.4.0), against seeded data. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class CrmContactsPipelineMailTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    MockMvc mvc;

    @Test
    void contactsListCreatePatch() throws Exception {
        // Seeded contact of a seeded customer.
        mvc.perform(get("/api/crm/contacts").param("customerId", "cust-kontor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("cust-kontor"));

        // Create.
        String created = mvc.perform(post("/api/crm/contacts").contentType(JSON).content(
                        "{\"customerId\":\"cust-kontor\",\"name\":\"Neu Person\",\"role\":\"Ops\","
                                + "\"email\":\"neu@kontor-nord.de\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Neu Person"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        // Patch (newsletter opt-in + role).
        mvc.perform(patch("/api/crm/contacts/{id}", id).contentType(JSON)
                        .content("{\"newsletter\":true,\"role\":\"Head of Ops\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsletter").value(true))
                .andExpect(jsonPath("$.role").value("Head of Ops"));

        mvc.perform(patch("/api/crm/contacts/{id}", "does-not-exist").contentType(JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pipelineListCreateLeadAndSetStage() throws Exception {
        mvc.perform(get("/api/crm/pipeline")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stage").exists())
                .andExpect(jsonPath("$[0].probability").exists());

        // Filter by customer.
        mvc.perform(get("/api/crm/pipeline").param("customerId", "cust-vintage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("cust-vintage"));

        // Create a bare lead → NEU / probability 15.
        String created = mvc.perform(post("/api/crm/pipeline").contentType(JSON).content(
                        "{\"company\":\"Probe GmbH\",\"customerId\":\"cust-kontor\",\"ownerInitials\":\"JS\","
                                + "\"value\":{\"amount\":5000,\"currency\":\"EUR\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("NEU"))
                .andExpect(jsonPath("$.probability").value(15))
                .andExpect(jsonPath("$.source").value("LEAD"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        // Drag/inline-edit → stage GEWONNEN, probability derives to 100.
        mvc.perform(patch("/api/crm/pipeline/{id}", id).contentType(JSON).content("{\"stage\":\"GEWONNEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("GEWONNEN"))
                .andExpect(jsonPath("$.probability").value(100));
    }

    @Test
    void mailThreadsByCustomerAndContact() throws Exception {
        String byCustomer = mvc.perform(get("/api/crm/threads").param("customerId", "cust-kontor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").exists())
                .andExpect(jsonPath("$[0].messageCount").value(3))
                .andReturn().getResponse().getContentAsString();
        String threadId = JsonPath.read(byCustomer, "$[0].id");

        mvc.perform(get("/api/crm/threads").param("contactId", "contact-vintage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactId").value("contact-vintage"));

        mvc.perform(get("/api/crm/threads/{id}", threadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].direction").exists())
                .andExpect(jsonPath("$.preview").exists());

        mvc.perform(get("/api/crm/threads/{id}", "does-not-exist")).andExpect(status().isNotFound());
    }
}
