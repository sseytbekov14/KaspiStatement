package com.sultan.kaspitracker.config;

import com.sultan.kaspitracker.controller.api.StatementApiController;
import com.sultan.kaspitracker.controller.web.WebController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({WebController.class})
@Import(SecurityConfig.class)
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void unauthenticatedAccess_WebUi_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/").accept(org.springframework.http.MediaType.TEXT_HTML))
               .andExpect(status().is3xxRedirection()); // redirects to /login
    }

    @Test
    public void unauthenticatedAccess_Api_ReturnsUnauthorized() throws Exception {
        // Since we are only loading WebController in this @WebMvcTest,
        // requests to /api/statements will hit a 404 handler. 
        // But Spring Security filter chain sits BEFORE the dispatcher servlet.
        // It should intercept /api/statements and return 401 Unauthorized
        mockMvc.perform(get("/api/statements"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void authenticatedAccess_WebUi_ReturnsOk() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk());
    }
}
