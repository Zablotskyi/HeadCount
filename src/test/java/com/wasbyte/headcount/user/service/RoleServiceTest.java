package com.wasbyte.headcount.user.service;

import com.wasbyte.headcount.user.entity.Role;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.repository.RoleRepository;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock RoleRepository roleRepository;
    @Mock UserRepository userRepository;
    @InjectMocks RoleService service;

    @Test
    void findByNameReturnsRole() {
        Role role = new Role();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        assertSame(role, service.findByName("ADMIN"));
    }

    @Test
    void addRoleAddsRole() {
        User user = userWithRoles();
        Role role = new Role();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        service.addRole(1L, "ADMIN");
        assertEquals(1, user.getRoles().size());
    }

    @Test
    void addingSameRoleTwiceDoesNotCreateDuplicate() {
        User user = userWithRoles();
        Role role = new Role();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        service.addRole(1L, "ADMIN");
        service.addRole(1L, "ADMIN");

        assertEquals(1, user.getRoles().size());
    }

    @Test
    void removeRoleRemovesRole() {
        User user = userWithRoles();
        Role role = org.mockito.Mockito.mock(Role.class);
        when(role.getId()).thenReturn(5L);
        user.getRoles().add(role);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        service.removeRole(1L, "ADMIN");
        assertEquals(0, user.getRoles().size());
    }

    private User userWithRoles() {
        User user = new User();
        user.setRoles(new HashSet<>());
        return user;
    }
}
