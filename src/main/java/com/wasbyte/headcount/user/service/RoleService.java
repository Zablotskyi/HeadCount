package com.wasbyte.headcount.user.service;

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
    public User removeRole(Long userId, String roleName) {
        User user = findUser(userId);
        Role role = findByName(roleName);
        user.getRoles().removeIf(existing -> existing.getId().equals(role.getId()));
        return user;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
