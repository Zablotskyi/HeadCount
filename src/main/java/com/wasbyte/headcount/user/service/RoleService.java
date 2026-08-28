package com.wasbyte.headcount.user.service;

import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.exception.ResourceNotFoundException;
import com.wasbyte.headcount.user.entity.Role;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.repository.RoleRepository;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public Role findByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));
    }

    public List<String> findAllNames() {
        return roleRepository.findAllByOrderByNameAsc().stream()
                .map(Role::getName)
                .toList();
    }

    @Transactional
    public User addRole(Long userId, String roleName) {
        User user = findUser(userId);
        user.getRoles().add(findByName(roleName));
        return user;
    }

    @Transactional
    public User removeRole(Long userId, String roleName, Long actorUserId) {
        if (ADMIN_ROLE.equals(roleName) && userId.equals(actorUserId)) {
            throw new InvalidOperationException("Не можна видалити роль ADMIN у власного облікового запису");
        }

        Role role = ADMIN_ROLE.equals(roleName)
                ? roleRepository.findByNameForUpdate(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName))
                : findByName(roleName);
        User user = findUser(userId);
        boolean assigned = user.getRoles().stream()
                .anyMatch(existing -> existing.getId().equals(role.getId()));
        if (ADMIN_ROLE.equals(roleName) && assigned
                && userRepository.countDistinctByRolesName(ADMIN_ROLE) <= 1) {
            throw new InvalidOperationException("Не можна видалити роль ADMIN в останнього адміністратора");
        }
        user.getRoles().removeIf(existing -> existing.getId().equals(role.getId()));
        return user;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
