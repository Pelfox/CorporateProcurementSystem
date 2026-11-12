package com.team.corporate.test.backend;

import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.CorrelationIdFilter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.team.corporate.backend.security.BusinessAccess;
import com.team.corporate.core.services.OrdersService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Existing filter-free domain regressions; authorization is exercised by the separate real-filter HTTP tests. */
abstract class LegacyBusinessIntegrationTest extends BackendIntegrationTest {
    @MockitoBean private BusinessAccess access;
    @Autowired private OrdersService orders;
    @Autowired private WebApplicationContext context;
    @Autowired private CorrelationIdFilter correlationIds;

    @BeforeEach
    void trustedDomainFixture() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(correlationIds).build();
        when(access.isManager()).thenReturn(true);
        when(access.actor(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(access.orderScope(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(access.requireOrder(any(UUID.class))).thenAnswer(invocation -> orders.getOrder(invocation.getArgument(0))
                .orElseThrow(() -> ApiException.notFound("Заказ")));
    }
}
