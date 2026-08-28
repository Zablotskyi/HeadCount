package com.wasbyte.headcount.api;

import com.wasbyte.headcount.exception.DuplicateResourceException;
import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.exception.ResourceNotFoundException;
import com.wasbyte.headcount.headcount.entity.HeadcountEvent;
import com.wasbyte.headcount.headcount.entity.HeadcountEventStatus;
import com.wasbyte.headcount.headcount.entity.HeadcountParticipant;
import com.wasbyte.headcount.headcount.entity.HeadcountParticipantStatus;
import com.wasbyte.headcount.headcount.service.HeadcountService;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.entity.OrganizationUnitType;
import com.wasbyte.headcount.organization.service.OrganizationUnitService;
import com.wasbyte.headcount.security.UserPrincipal;
import com.wasbyte.headcount.user.entity.Role;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.entity.UserStatus;
import com.wasbyte.headcount.user.service.RoleService;
import com.wasbyte.headcount.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class ApiControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean OrganizationUnitService organizationUnitService;
    @MockitoBean UserService userService;
    @MockitoBean RoleService roleService;
    @MockitoBean HeadcountService headcountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void csrfEndpointReturnsTokenMetadata() throws Exception {
        mockMvc.perform(get("/api/csrf").with(user(principal(7L, "EMPLOYEE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Nested
    class OrganizationApi {

        @Test
        void rootsReturn200() throws Exception {
            when(organizationUnitService.getRoots()).thenReturn(List.of(unit("Global", "GLOBAL")));

            mockMvc.perform(get("/api/organization-units/roots")
                            .with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Global"));
        }

        @Test
        void getExistingReturns200() throws Exception {
            when(organizationUnitService.findById(1L)).thenReturn(unit("Kyiv", "KYIV"));

            mockMvc.perform(get("/api/organization-units/1").with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Kyiv"));
        }

        @Test
        void getMissingReturns404WithApiError() throws Exception {
            when(organizationUnitService.findById(404L))
                    .thenThrow(new ResourceNotFoundException("Organization unit not found"));

            mockMvc.perform(get("/api/organization-units/404").with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.path").value("/api/organization-units/404"));
        }

        @Test
        void validCreateReturns201() throws Exception {
            when(organizationUnitService.create(any())).thenReturn(unit("Kyiv", "KYIV"));

            mockMvc.perform(post("/api/organization-units")
                            .with(user(principal(1L, "ADMIN"))).with(csrf())
                            .contentType("application/json")
                            .content("""
                                    {"name":"Kyiv","code":"KYIV","type":"OFFICE","sortOrder":1}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("OFFICE"));
        }

        @Test
        void invalidCreateReturnsStructured400() throws Exception {
            mockMvc.perform(post("/api/organization-units")
                            .with(user(principal(1L, "ADMIN"))).with(csrf())
                            .contentType("application/json")
                            .content("{\"name\":\"\",\"code\":\"\",\"type\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.validationErrors.name").exists())
                    .andExpect(jsonPath("$.validationErrors.type").exists());
        }

        @Test
        void hierarchyBusinessErrorReturns400() throws Exception {
            when(organizationUnitService.changeParent(1L, 2L))
                    .thenThrow(new InvalidOperationException("Hierarchy cycle"));

            mockMvc.perform(patch("/api/organization-units/1/parent")
                            .with(user(principal(1L, "ADMIN"))).with(csrf())
                            .contentType("application/json").content("{\"parentId\":2}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Hierarchy cycle"));
        }

        @Test
        void employeeCannotCreateUnit() throws Exception {
            mockMvc.perform(post("/api/organization-units")
                            .with(user(principal(7L, "EMPLOYEE"))).with(csrf())
                            .contentType("application/json")
                            .content("""
                                    {"name":"Kyiv","code":"KYIV","type":"OFFICE","sortOrder":1}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class UserApi {

        @Test
        void currentUserComesFromPrincipalId() throws Exception {
            User found = mock(User.class);
            Role employee = new Role(); employee.setName("EMPLOYEE");
            when(found.getId()).thenReturn(77L);
            when(found.getUsername()).thenReturn("jsmith");
            when(found.getFirstName()).thenReturn("John");
            when(found.getLastName()).thenReturn("Smith");
            when(found.getRoles()).thenReturn(new HashSet<>(Set.of(employee)));
            when(userService.findById(77L)).thenReturn(found);

            mockMvc.perform(get("/api/users/me").with(user(principal(77L, "EMPLOYEE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(77))
                    .andExpect(jsonPath("$.username").value("jsmith"))
                    .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"));
            verify(userService).findById(77L);
        }

        @Test
        void adminCanListAndSearchUsers() throws Exception {
            when(userService.findAll()).thenReturn(List.of(userEntity("jsmith")));
            when(userService.search("john")).thenReturn(List.of(userEntity("jsmith")));

            mockMvc.perform(get("/api/users").with(user(principal(1L, "ADMIN"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("jsmith"));
            mockMvc.perform(get("/api/users/search?q=john").with(user(principal(1L, "ADMIN"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("jsmith"));
        }

        @Test
        void employeeCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users").with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getExistingReturns200WithoutPasswordHash() throws Exception {
            User found = userEntity("jsmith");
            found.setPasswordHash("secret-hash");
            when(userService.findById(1L)).thenReturn(found);

            mockMvc.perform(get("/api/users/1").with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("jsmith"))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        void createEncodesPasswordBeforeServiceCall() throws Exception {
            when(userService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            mockMvc.perform(post("/api/users")
                            .with(user(principal(1L, "ADMIN"))).with(csrf())
                            .contentType("application/json").content(validUserJson()))
                    .andExpect(status().isCreated());

            var captor = org.mockito.ArgumentCaptor.forClass(User.class);
            verify(userService).create(captor.capture());
            String passwordHash = captor.getValue().getPasswordHash();
            org.junit.jupiter.api.Assertions.assertTrue(passwordHash.startsWith("$2"));
            org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("strong-password", passwordHash));
        }

        @Test
        void duplicateUserReturns409() throws Exception {
            when(userService.create(any())).thenThrow(new DuplicateResourceException("Username already exists"));

            mockMvc.perform(post("/api/users")
                            .with(user(principal(1L, "ADMIN"))).with(csrf())
                            .contentType("application/json").content(validUserJson()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"));
        }

        @Test
        void invalidEmailReturns400() throws Exception {
            mockMvc.perform(post("/api/users")
                            .with(user(principal(1L, "ADMIN"))).with(csrf())
                            .contentType("application/json")
                            .content(validUserJson().replace("john@example.com", "not-email")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.email").exists());
        }

        @Test
        void adminCanAssignRole() throws Exception {
            User result = userEntity("jsmith");
            Role admin = new Role();
            admin.setName("ADMIN");
            result.setRoles(new HashSet<>(Set.of(admin)));
            when(roleService.addRole(1L, "ADMIN")).thenReturn(result);

            mockMvc.perform(post("/api/users/1/roles")
                            .with(user(principal(1L, "ADMIN"))).with(csrf())
                            .contentType("application/json").content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
        }

        @Test
        void employeeCannotAssignRole() throws Exception {
            mockMvc.perform(post("/api/users/1/roles")
                            .with(user(principal(7L, "EMPLOYEE"))).with(csrf())
                            .contentType("application/json").content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void removeRoleUsesAuthenticatedPrincipalId() throws Exception {
            User result = userEntity("target");
            when(roleService.removeRole(2L, "ADMIN", 1L)).thenReturn(result);

            mockMvc.perform(delete("/api/users/2/roles/ADMIN")
                            .with(user(principal(1L, "ADMIN"))).with(csrf()))
                    .andExpect(status().isOk());

            verify(roleService).removeRole(2L, "ADMIN", 1L);
        }

        @Test
        void selfAdminRemovalBusinessErrorReturns400() throws Exception {
            when(roleService.removeRole(1L, "ADMIN", 1L)).thenThrow(new InvalidOperationException(
                    "Не можна видалити роль ADMIN у власного облікового запису"));

            mockMvc.perform(delete("/api/users/1/roles/ADMIN")
                            .with(user(principal(1L, "ADMIN"))).with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Не можна видалити роль ADMIN у власного облікового запису"));
        }

        @Test
        void lastAdminRemovalBusinessErrorReturns400() throws Exception {
            when(roleService.removeRole(2L, "ADMIN", 1L)).thenThrow(new InvalidOperationException(
                    "Не можна видалити роль ADMIN в останнього адміністратора"));

            mockMvc.perform(delete("/api/users/2/roles/ADMIN")
                            .with(user(principal(1L, "ADMIN"))).with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Не можна видалити роль ADMIN в останнього адміністратора"));
        }
    }

    @Nested
    class RoleApi {

        @Test
        void adminCanListRolesIncludingHeadcountManager() throws Exception {
            when(roleService.findAllNames()).thenReturn(List.of("ADMIN", "EMPLOYEE", "HEADCOUNT_MANAGER"));

            mockMvc.perform(get("/api/roles")
                            .with(user(principal(1L, "ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[2]").value("HEADCOUNT_MANAGER"));
        }

        @Test
        void employeeCannotListRoles() throws Exception {
            mockMvc.perform(get("/api/roles")
                            .with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticatedRoleListRedirectsToLogin() throws Exception {
            mockMvc.perform(get("/api/roles"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    class HeadcountApi {

        @Test
        void activeEventLookupReturnsEventOr204() throws Exception {
            when(headcountService.findActiveEvent(10L)).thenReturn(Optional.of(event()));
            when(headcountService.findActiveEvent(11L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/headcount/events/active?scopeOrganizationUnitId=10")
                            .with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
            mockMvc.perform(get("/api/headcount/events/active?scopeOrganizationUnitId=11")
                            .with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        void headcountManagerCanCreateEventAndPrincipalIdIsUsed() throws Exception {
            HeadcountEvent event = event();
            when(headcountService.createEvent("Alarm", "Test", 10L, 77L)).thenReturn(event);

            mockMvc.perform(post("/api/headcount/events")
                            .with(user(principal(77L, "HEADCOUNT_MANAGER"))).with(csrf())
                            .contentType("application/json")
                            .content("""
                                    {"title":"Alarm","description":"Test","scopeOrganizationUnitId":10}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
            verify(headcountService).createEvent("Alarm", "Test", 10L, 77L);
        }

        @Test
        void participantListReturns200() throws Exception {
            when(headcountService.findParticipants(3L)).thenReturn(List.of(participant()));

            mockMvc.perform(get("/api/headcount/events/3/participants")
                            .with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        void authenticatedUserCanGetLimitedParticipantDetails() throws Exception {
            HeadcountParticipant participant = participant();
            User employee = participant.getEmployee();
            employee.setFirstName("Oksana");
            employee.setLastName("Polishchuk");
            employee.setPosition("ICT Manager");
            employee.setEmail("oksana@example.com");
            employee.setMobileNumber("+380501234567");
            employee.setCountry("Ukraine");
            employee.setCity("Chernihiv");
            employee.setOffice("Chernihiv FO");
            employee.setAddress("1 Peace Street");
            when(headcountService.findParticipantDetails(3L, 10L)).thenReturn(participant);

            mockMvc.perform(get("/api/headcount/events/3/participants/10")
                            .with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Oksana"))
                    .andExpect(jsonPath("$.lastName").value("Polishchuk"))
                    .andExpect(jsonPath("$.position").value("ICT Manager"))
                    .andExpect(jsonPath("$.email").value("oksana@example.com"))
                    .andExpect(jsonPath("$.mobileNumber").value("+380501234567"))
                    .andExpect(jsonPath("$.country").value("Ukraine"))
                    .andExpect(jsonPath("$.city").value("Chernihiv"))
                    .andExpect(jsonPath("$.office").value("Chernihiv FO"))
                    .andExpect(jsonPath("$.address").value("1 Peace Street"))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.roles").doesNotExist())
                    .andExpect(jsonPath("$.enabled").doesNotExist())
                    .andExpect(jsonPath("$.status").doesNotExist())
                    .andExpect(jsonPath("$.resourceNumber").doesNotExist())
                    .andExpect(jsonPath("$.authorizedPersonPhoneNumber").doesNotExist());
        }

        @Test
        void participantFromAnotherEventReturns404() throws Exception {
            when(headcountService.findParticipantDetails(3L, 20L))
                    .thenThrow(new ResourceNotFoundException("Participant not found"));

            mockMvc.perform(get("/api/headcount/events/3/participants/20")
                            .with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void unknownParticipantReturns404() throws Exception {
            when(headcountService.findParticipantDetails(3L, 404L))
                    .thenThrow(new ResourceNotFoundException("Participant not found"));

            mockMvc.perform(get("/api/headcount/events/3/participants/404")
                            .with(user(principal(7L, "EMPLOYEE"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void unauthenticatedParticipantDetailsRedirectsToLogin() throws Exception {
            mockMvc.perform(get("/api/headcount/events/3/participants/10"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        void safeConfirmationUsesPrincipalId() throws Exception {
            HeadcountParticipant participant = participant();
            participant.setStatus(HeadcountParticipantStatus.SAFE);
            when(headcountService.confirmSafe(3L, 9L, 77L, "SELF")).thenReturn(participant);

            mockMvc.perform(post("/api/headcount/events/3/participants/9/safe")
                            .with(user(principal(77L, "EMPLOYEE"))).with(csrf())
                            .contentType("application/json")
                            .content("{\"confirmationSource\":\"SELF\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SAFE"));
            verify(headcountService).confirmSafe(3L, 9L, 77L, "SELF");
        }

        @Test
        void needHelpRequiresMessage() throws Exception {
            mockMvc.perform(post("/api/headcount/events/3/participants/9/need-help")
                            .with(user(principal(77L, "EMPLOYEE"))).with(csrf())
                            .contentType("application/json")
                            .content("{\"confirmationSource\":\"SELF\",\"helpMessage\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.helpMessage").exists());
        }

        @Test
        void closedOrCancelledBusinessErrorReturns400() throws Exception {
            when(headcountService.confirmSafe(3L, 9L, 77L, "SELF"))
                    .thenThrow(new InvalidOperationException("Event is closed"));

            mockMvc.perform(post("/api/headcount/events/3/participants/9/safe")
                            .with(user(principal(77L, "EMPLOYEE"))).with(csrf())
                            .contentType("application/json")
                            .content("{\"confirmationSource\":\"SELF\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Event is closed"));
        }

        @Test
        void duplicateEventReturns409() throws Exception {
            when(headcountService.createEvent("Alarm", null, 10L, 77L))
                    .thenThrow(new DuplicateResourceException("Active event already exists"));

            mockMvc.perform(post("/api/headcount/events")
                            .with(user(principal(77L, "ADMIN"))).with(csrf())
                            .contentType("application/json")
                            .content("{\"title\":\"Alarm\",\"scopeOrganizationUnitId\":10}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        void adminCanCreateEvent() throws Exception {
            when(headcountService.createEvent("Alarm", null, 10L, 1L)).thenReturn(event());

            mockMvc.perform(post("/api/headcount/events")
                            .with(user(principal(1L, "ADMIN"))).with(csrf())
                            .contentType("application/json")
                            .content("{\"title\":\"Alarm\",\"scopeOrganizationUnitId\":10}"))
                    .andExpect(status().isCreated());
        }

        @Test
        void departmentManagerCannotCreateEventWithoutHeadcountManagerRole() throws Exception {
            mockMvc.perform(post("/api/headcount/events")
                            .with(user(principal(7L, "DEPARTMENT_MANAGER"))).with(csrf())
                            .contentType("application/json")
                            .content("{\"title\":\"Alarm\",\"scopeOrganizationUnitId\":10}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void employeeCannotCreateEvent() throws Exception {
            mockMvc.perform(post("/api/headcount/events")
                            .with(user(principal(7L, "EMPLOYEE"))).with(csrf())
                            .contentType("application/json")
                            .content("{\"title\":\"Alarm\",\"scopeOrganizationUnitId\":10}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void headcountManagerCanCloseAndCancel() throws Exception {
            when(headcountService.closeEvent(3L, 77L)).thenReturn(event());
            when(headcountService.cancelEvent(4L, 77L)).thenReturn(event());

            mockMvc.perform(post("/api/headcount/events/3/close")
                            .with(user(principal(77L, "EMPLOYEE", "HEADCOUNT_MANAGER"))).with(csrf()))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/headcount/events/4/cancel")
                            .with(user(principal(77L, "HEADCOUNT_MANAGER"))).with(csrf()))
                    .andExpect(status().isOk());
            verify(headcountService).closeEvent(3L, 77L);
            verify(headcountService).cancelEvent(4L, 77L);
        }

        @Test
        void adminCanCloseAndCancel() throws Exception {
            when(headcountService.closeEvent(3L, 1L)).thenReturn(event());
            when(headcountService.cancelEvent(4L, 1L)).thenReturn(event());

            mockMvc.perform(post("/api/headcount/events/3/close")
                            .with(user(principal(1L, "ADMIN"))).with(csrf()))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/headcount/events/4/cancel")
                            .with(user(principal(1L, "ADMIN"))).with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        void previousManagementRolesCannotCloseOrCancel() throws Exception {
            mockMvc.perform(post("/api/headcount/events/3/close")
                            .with(user(principal(7L, "DEPARTMENT_MANAGER"))).with(csrf()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/headcount/events/3/cancel")
                            .with(user(principal(8L, "SECURITY_MANAGER"))).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void employeeCannotCloseOrCancel() throws Exception {
            mockMvc.perform(post("/api/headcount/events/3/close")
                            .with(user(principal(7L, "EMPLOYEE"))).with(csrf()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/headcount/events/3/cancel")
                            .with(user(principal(7L, "EMPLOYEE"))).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    private OrganizationUnit unit(String name, String code) {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setName(name);
        unit.setCode(code);
        unit.setType(OrganizationUnitType.OFFICE);
        unit.setActive(true);
        return unit;
    }

    private User userEntity(String username) {
        User user = new User();
        user.setUsername(username);
        user.setResourceNumber("R-1");
        user.setFirstName("John");
        user.setLastName("Smith");
        user.setEmail("john@example.com");
        user.setTimeZone("Europe/Kyiv");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new HashSet<>());
        return user;
    }

    private UserPrincipal principal(Long id, String... roles) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn("test-user");
        when(user.getPasswordHash()).thenReturn("$2a$10$encoded");
        when(user.isEnabled()).thenReturn(true);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        Set<Role> roleSet = new HashSet<>();
        for (String name : roles) {
            Role role = new Role();
            role.setName(name);
            roleSet.add(role);
        }
        when(user.getRoles()).thenReturn(roleSet);
        return new UserPrincipal(user);
    }

    private HeadcountEvent event() {
        HeadcountEvent event = new HeadcountEvent();
        event.setTitle("Alarm");
        event.setDescription("Test");
        event.setStatus(HeadcountEventStatus.ACTIVE);
        event.setScopeOrganizationUnit(unit("Kyiv", "KYIV"));
        event.setStartedAt(LocalDateTime.now());
        event.setStartedBy(userEntity("starter"));
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        return event;
    }

    private HeadcountParticipant participant() {
        HeadcountParticipant participant = new HeadcountParticipant();
        participant.setEvent(event());
        participant.setEmployee(userEntity("employee"));
        participant.setEmployeeNameSnapshot("John Smith");
        participant.setResourceNumberSnapshot("R-1");
        participant.setOrganizationPathSnapshot("Organization / Kyiv");
        participant.setStatus(HeadcountParticipantStatus.PENDING);
        return participant;
    }

    private String validUserJson() {
        return """
                {
                  "username":"jsmith",
                  "resourceNumber":"R-1",
                  "firstName":"John",
                  "lastName":"Smith",
                  "email":"john@example.com",
                  "password":"strong-password",
                  "timeZone":"Europe/Kyiv"
                }
                """;
    }
}
