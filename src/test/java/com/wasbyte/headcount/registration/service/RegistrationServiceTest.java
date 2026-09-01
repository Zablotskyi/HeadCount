package com.wasbyte.headcount.registration.service;

import com.wasbyte.headcount.exception.DuplicateResourceException;
import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.exception.ResourceNotFoundException;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.service.OrganizationUnitService;
import com.wasbyte.headcount.registration.dto.EmployeeRegistrationRequest;
import com.wasbyte.headcount.user.entity.Role;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.entity.UserStatus;
import com.wasbyte.headcount.user.service.RoleService;
import com.wasbyte.headcount.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock UserService userService;
    @Mock RoleService roleService;
    @Mock OrganizationUnitService organizationUnitService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks RegistrationService registrationService;

    private OrganizationUnit activeUnit;
    private Role employeeRole;

    @BeforeEach
    void setUp() {
        activeUnit = org.mockito.Mockito.mock(OrganizationUnit.class);
        employeeRole = new Role();
        employeeRole.setName("EMPLOYEE");
    }

    @Test
    void registrationCreatesPendingDisabledEmployee() {
        arrangeSuccessfulRegistration();

        registrationService.register(request("password123", "password123"));

        User user = capturedUser();
        assertSame(UserStatus.PENDING_APPROVAL, user.getStatus());
        assertFalse(user.isEnabled());
        assertSame(employeeRole, user.getRoles().iterator().next());
    }

    @Test
    void passwordIsEncoded() {
        arrangeSuccessfulRegistration();

        registrationService.register(request("password123", "password123"));

        User user = capturedUser();
        assertNotEquals("password123", user.getPasswordHash());
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void passwordMismatchRejected() {
        assertThrows(InvalidOperationException.class,
                () -> registrationService.register(request("password123", "different123")));
        verify(userService, never()).create(any());
    }

    @Test
    void duplicateUsernameRejected() {
        arrangeDependencies();
        when(userService.create(any())).thenThrow(new DuplicateResourceException("Username already exists"));

        assertThrows(DuplicateResourceException.class,
                () -> registrationService.register(request("password123", "password123")));
    }

    @Test
    void duplicateEmailRejected() {
        arrangeDependencies();
        when(userService.create(any())).thenThrow(new DuplicateResourceException("Email already exists"));

        assertThrows(DuplicateResourceException.class,
                () -> registrationService.register(request("password123", "password123")));
    }

    @Test
    void invalidOrganizationUnitRejected() {
        when(organizationUnitService.findById(5L))
                .thenThrow(new ResourceNotFoundException("Organization unit not found: 5"));

        assertThrows(ResourceNotFoundException.class,
                () -> registrationService.register(request("password123", "password123")));
        verify(userService, never()).create(any());
    }

    @Test
    void inactiveOrganizationUnitRejected() {
        when(activeUnit.isActive()).thenReturn(false);
        when(organizationUnitService.findById(5L)).thenReturn(activeUnit);

        assertThrows(InvalidOperationException.class,
                () -> registrationService.register(request("password123", "password123")));
        verify(userService, never()).create(any());
    }

    private void arrangeSuccessfulRegistration() {
        arrangeDependencies();
        when(userService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void arrangeDependencies() {
        when(activeUnit.isActive()).thenReturn(true);
        when(organizationUnitService.findById(5L)).thenReturn(activeUnit);
        when(roleService.findByName("EMPLOYEE")).thenReturn(employeeRole);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
    }

    private User capturedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).create(captor.capture());
        return captor.getValue();
    }

    private EmployeeRegistrationRequest request(String password, String confirmation) {
        return new EmployeeRegistrationRequest(
                "new.employee", password, confirmation, "R-100", "New", "Employee",
                "Engineer", "new@example.com", "+380000000000", "Ukraine", "Chernihiv",
                "FO", "Address", "+380000000001", "Europe/Kyiv", 5L);
    }
}
