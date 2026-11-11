package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BackendMain.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:security_boundary;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@ContextConfiguration(initializers = JwtTestKeys.class)
class SecurityBoundaryTest {
    @Autowired private MockMvc mvc;

    @Test
    void anonymousRequestsCannotUseLegacyApiOrObtainServletSessions() throws Exception {
        for (String endpoint : new String[]{"/api/users", "/api/users/by-username?username=test", "/api/orders"}) {
            mvc.perform(get(endpoint)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Требуется аутентификация."))
                    .andExpect(header().doesNotExist("Set-Cookie"));
        }
    }
}
