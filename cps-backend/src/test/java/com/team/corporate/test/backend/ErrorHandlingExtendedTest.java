package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ErrorHandlingExtendedTest extends BackendIntegrationTest {

    @Test
    void errorResponseContainsCorrelationId() throws Exception {
        var result = mvc.perform(get("/api/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        String headerId = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(headerId).isNotNull().isNotEmpty();

        var body = mapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.path("correlationId").asString()).isEqualTo(headerId);
    }

    @Test
    void customCorrelationIdIsPreservedInResponse() throws Exception {
        String customId = "my-custom-correlation-id-42";
        var result = mvc.perform(get("/api/orders/" + UUID.randomUUID())
                        .header("X-Correlation-ID", customId))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-ID", customId))
                .andReturn();

        var body = mapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.path("correlationId").asString()).isEqualTo(customId);
    }

    @Test
    void notFoundReturns404WithUnifiedJson() throws Exception {
        UUID missing = UUID.randomUUID();
        var result = mvc.perform(get("/api/orders/" + missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("stackTrace", "Exception", "at java.");
    }

    @Test
    void validationErrorReturns400WithViolationsAndCorrelationId() throws Exception {
        mvc.perform(post("/api/orders/bulk")
                        .param("actingUserId", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.violations").isNotEmpty())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void orderDtoIncludesVersionAndArchiveFields() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        UUID orderId = id(order(buyer, null, line(product, 1)));

        var orderJson = read("/api/orders/" + orderId);
        assertThat(orderJson.path("version").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(orderJson.path("archived").asBoolean()).isFalse();
        assertThat(orderJson.path("tags").isArray()).isTrue();
    }
}