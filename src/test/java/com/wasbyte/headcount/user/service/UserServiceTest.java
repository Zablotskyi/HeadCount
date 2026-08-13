package com.wasbyte.headcount.user.service;

import com.wasbyte.headcount.exception.DuplicateResourceException;
import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.repository.OrganizationUnitRepository;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.entity.UserStatus;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrganizationUnitRepository unitRepository;
    @InjectMocks UserService service;

    @Test
    void createUser() {
        User user = newUser();
        when(userRepository.save(user)).thenReturn(user);

        assertSame(user, service.create(user));
        verify(userRepository).save(user);
    }

    @Test
    void createRejectsDuplicateUsername() {
        User user = newUser();
        User existing = existingUser(1L);
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(existing));

        assertThrows(DuplicateResourceException.class, () -> service.create(user));
    }

    @Test
    void createRejectsDuplicateEmail() {
        User user = newUser();
        User existing = existingUser(1L);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(existing));

        assertThrows(DuplicateResourceException.class, () -> service.create(user));
    }

    @Test
    void createRejectsDuplicateResourceNumber() {
        User user = newUser();
        User existing = existingUser(1L);
        when(userRepository.findByResourceNumber(user.getResourceNumber())).thenReturn(Optional.of(existing));

        assertThrows(DuplicateResourceException.class, () -> service.create(user));
    }

    @Test
    void createRejectsPlaintextPassword() {
        User user = newUser();
        user.setPasswordHash("plain-secret");

        assertThrows(InvalidOperationException.class, () -> service.create(user));
    }

    @Test
    void assignOrganizationUnit() {
        User user = mock(User.class);
        OrganizationUnit unit = mock(OrganizationUnit.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(unitRepository.findById(2L)).thenReturn(Optional.of(unit));

        service.assignOrganizationUnit(1L, 2L);
        verify(user).setOrganizationUnit(unit);
    }

    @Test
    void assignLineManagerRejectsSelf() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));

        assertThrows(InvalidOperationException.class, () -> service.assignLineManager(1L, 1L));
    }

    @Test
    void assignLineManagerRejectsCycle() {
        User user = mockUser(1L, null);
        User manager = mockUser(2L, user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        assertThrows(InvalidOperationException.class, () -> service.assignLineManager(1L, 2L));
    }

    @Test
    void setAccountActiveActivatesAndDeactivates() {
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.setAccountActive(1L, true);
        assertTrue(user.isEnabled());
        assertSame(UserStatus.ACTIVE, user.getStatus());

        service.setAccountActive(1L, false);
        assertFalse(user.isEnabled());
        assertSame(UserStatus.SUSPENDED, user.getStatus());
    }

    @Test
    void changeStatusUpdatesStatus() {
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertSame(user, service.changeStatus(1L, UserStatus.ARCHIVED));
        assertSame(UserStatus.ARCHIVED, user.getStatus());
    }

    private User newUser() {
        User user = new User();
        user.setUsername("jsmith");
        user.setEmail("john@example.com");
        user.setResourceNumber("R-1");
        user.setPasswordHash("{bcrypt}$2a$10$encoded");
        return user;
    }

    private User existingUser(Long id) {
        return mockUser(id, null);
    }

    private User mockUser(Long id, User manager) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLineManager()).thenReturn(manager);
        return user;
    }
}
