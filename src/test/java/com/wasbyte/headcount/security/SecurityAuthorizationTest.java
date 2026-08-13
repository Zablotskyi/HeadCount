package com.wasbyte.headcount.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Import(SecurityAuthorizationTest.TestEndpointsConfiguration.class)
class SecurityAuthorizationTest {

    @Autowired WebApplicationContext context;
    @Autowired PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void usesBCryptPasswordEncoder() {
        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
    }

    @Test
    void unauthenticatedRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/test"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SECURITY_OFFICER")
    void securityOfficerCanAccessSecurityEndpoint() throws Exception {
        mockMvc.perform(get("/security/test"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotAccessSecurityEndpoint() throws Exception {
        mockMvc.perform(get("/security/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DEPARTMENT_MANAGER")
    void managerCanAccessHeadcountManagement() throws Exception {
        mockMvc.perform(get("/headcount/manage/test"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCanAccessHeadcountEndpoint() throws Exception {
        mockMvc.perform(get("/headcount/test"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void csrfProtectsStateChangingRequest() throws Exception {
        mockMvc.perform(post("/profile/update"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestEndpointsConfiguration {

        @Bean
        TestEndpoints testEndpoints() {
            return new TestEndpoints();
        }
    }

    @RestController
    static class TestEndpoints {

        @GetMapping({"/profile/test", "/admin/test", "/security/test",
                "/headcount/manage/test", "/headcount/test"})
        String get() {
            return "ok";
        }

        @PostMapping("/profile/update")
        String post() {
            return "ok";
        }
    }
}
