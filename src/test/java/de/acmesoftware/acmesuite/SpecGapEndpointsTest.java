package de.acmesoftware.acmesuite;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contract test of the newly implemented spec-gap endpoints (v1): crm production + reset,
 * supply stock per material + reset. {@code @Transactional} rolls back the (partly destructive)
 * changes after each test so that the shared test context stays clean.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class SpecGapEndpointsTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    MockMvc mvc;

    @Test
    void supplyStockReadConsumeReset() throws Exception {
        String materials = mvc.perform(get("/api/supply/materials")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String matId = JsonPath.read(materials, "$[0].id");

        mvc.perform(get("/api/supply/materials/{id}/stock", matId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materialId").value(matId))
                .andExpect(jsonPath("$.available").exists());

        mvc.perform(post("/api/supply/materials/{id}/stock/consumption", matId)
                        .contentType(JSON).content("{\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materialId").value(matId));

        mvc.perform(post("/api/supply/stock/reset")).andExpect(status().isNoContent());

        mvc.perform(get("/api/supply/materials/{id}/stock", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crmRecordProductionThenDeleteAll() throws Exception {
        // Create an order (1× prod-radio for a seeded direct customer).
        String created = mvc.perform(post("/api/crm/orders").contentType(JSON)
                        .content("{\"customerId\":\"cust-kontor\",\"lines\":[{\"productId\":\"prod-radio\",\"quantity\":1}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(created, "$.id");

        // Submit; if approval is required, approve with the resolved authorized signatory.
        String submitted = mvc.perform(post("/api/crm/orders/{id}/submission", orderId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        if (!"APPROVED".equals(JsonPath.read(submitted, "$.status"))) {
            String approverId = JsonPath.read(submitted, "$.approval.approverId");
            mvc.perform(post("/api/crm/orders/{id}/decision", orderId).contentType(JSON)
                            .content("{\"approverId\":\"" + approverId + "\",\"decision\":\"APPROVE\"}"))
                    .andExpect(status().isOk());
        }

        // Record production → line fully fulfilled, order FULFILLED.
        mvc.perform(post("/api/crm/orders/{id}/production", orderId).contentType(JSON)
                        .content("{\"lines\":[{\"productId\":\"prod-radio\",\"producedQuantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.lines[0].remainingQuantity").value(0));

        // Reset: delete all orders.
        mvc.perform(delete("/api/crm/orders")).andExpect(status().isNoContent());
        mvc.perform(get("/api/crm/orders")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
