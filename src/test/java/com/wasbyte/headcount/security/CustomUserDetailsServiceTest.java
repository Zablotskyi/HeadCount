package com.wasbyte.headcount.security;

import com.wasbyte.headcount.user.entity.Role;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.entity.UserStatus;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks CustomUserDetailsService service;

    @Test
    void loadsExistingActiveEnabledUser() {
        User user = user(true, UserStatus.ACTIVE, "EMPLOYEE");
        when(userRepository.findByUsername("jsmith")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("jsmith");

        assertEquals("jsmith", details.getUsername());
        assertEquals("$2a$10$encoded", details.getPassword());
        assertTrue(details.isEnabled());
    }

    @Test
    void throwsWhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing"));
    }

    @Test
    void disabledUserIsNotEnabledForAuthentication() {
        User user = user(false, UserStatus.ACTIVE, "EMPLOYEE");
        when(userRepository.findByUsername("jsmith")).thenReturn(Optional.of(user));

        assertFalse(service.loadUserByUsername("jsmith").isEnabled());
    }

    @Test
    void nonActiveUserIsNotEnabledForAuthentication() {
        User user = user(true, UserStatus.PENDING_APPROVAL, "EMPLOYEE");
        when(userRepository.findByUsername("jsmith")).thenReturn(Optional.of(user));

        assertFalse(service.loadUserByUsername("jsmith").isEnabled());
    }

    @Test
    void roleNamesBecomeRoleAuthorities() {
        User user = user(true, UserStatus.ACTIVE, "ADMIN");
        when(userRepository.findByUsername("jsmith")).thenReturn(Optional.of(user));

        Set<String> authorities = authorityNames(service.loadUserByUsername("jsmith"));

        assertEquals(Set.of("ROLE_ADMIN"), authorities);
    }

    @Test
    void supportsMultipleRoles() {
        User user = user(true, UserStatus.ACTIVE, "EMPLOYEE", "SECURITY_OFFICER");
        when(userRepository.findByUsername("jsmith")).thenReturn(Optional.of(user));

        Set<String> authorities = authorityNames(service.loadUserByUsername("jsmith"));

        assertEquals(Set.of("ROLE_EMPLOYEE", "ROLE_SECURITY_OFFICER"), authorities);
    }

    private Set<String> authorityNames(UserDetails details) {
        Set<String> names = new HashSet<>();
        details.getAuthorities().forEach(authority -> names.add(authority.getAuthority()));
        return names;
    }

    private User user(boolean enabled, UserStatus status, String... roleNames) {
        User user = new User();
        user.setUsername("jsmith");
        user.setPasswordHash("$2a$10$encoded");
        user.setEnabled(enabled);
        user.setStatus(status);
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setName(roleName);
            roles.add(role);
        }
        user.setRoles(roles);
        return user;
    }
}
