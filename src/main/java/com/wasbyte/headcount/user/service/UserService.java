package com.wasbyte.headcount.user.service;

import com.wasbyte.headcount.exception.DuplicateResourceException;
import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.exception.ResourceNotFoundException;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.repository.OrganizationUnitRepository;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.entity.UserStatus;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationUnitRepository organizationUnitRepository;

    public UserService(UserRepository userRepository,
                       OrganizationUnitRepository organizationUnitRepository) {
        this.userRepository = userRepository;
        this.organizationUnitRepository = organizationUnitRepository;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found by username: " + username));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found by email: " + email));
    }

    public User findByResourceNumber(String resourceNumber) {
        return userRepository.findByResourceNumber(resourceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found by resource number: " + resourceNumber));
    }

    @Transactional
    public User create(User user) {
        if (user.getId() != null) {
            throw new InvalidOperationException("A new user must not already have an id");
        }
        assertUniqueIdentity(user, null);
        assertEncodedPassword(user.getPasswordHash());
        resolveRelationships(user);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.PENDING_EMAIL_VERIFICATION);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long userId, User profile) {
        User user = findById(userId);
        assertUniqueIdentity(profile, userId);

        user.setUsername(profile.getUsername());
        user.setResourceNumber(profile.getResourceNumber());
        user.setGrade(profile.getGrade());
        user.setFirstName(profile.getFirstName());
        user.setLastName(profile.getLastName());
        user.setMobileNumber(profile.getMobileNumber());
        user.setEmail(profile.getEmail());
        user.setCountry(profile.getCountry());
        user.setCity(profile.getCity());
        user.setOffice(profile.getOffice());
        user.setPosition(profile.getPosition());
        user.setAddress(profile.getAddress());
        user.setAuthorizedPersonPhoneNumber(profile.getAuthorizedPersonPhoneNumber());
        user.setTimeZone(profile.getTimeZone());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Transactional
    public User assignOrganizationUnit(Long userId, Long organizationUnitId) {
        User user = findById(userId);
        OrganizationUnit unit = organizationUnitId == null ? null : organizationUnitRepository.findById(organizationUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization unit not found: " + organizationUnitId));
        user.setOrganizationUnit(unit);
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Transactional
    public User assignLineManager(Long userId, Long managerId) {
        User user = findById(userId);
        if (userId.equals(managerId)) {
            throw new InvalidOperationException("A user cannot be their own line manager");
        }
        User manager = managerId == null ? null : findById(managerId);
        validateManagerChain(user, manager);
        user.setLineManager(manager);
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Transactional
    public User setAccountActive(Long userId, boolean active) {
        User user = findById(userId);
        user.setEnabled(active);
        user.setStatus(active ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Transactional
    public User changeStatus(Long userId, UserStatus status) {
        if (status == null) {
            throw new InvalidOperationException("User status is required");
        }
        User user = findById(userId);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private void resolveRelationships(User user) {
        if (user.getOrganizationUnit() != null) {
            user.setOrganizationUnit(organizationUnitRepository.findById(user.getOrganizationUnit().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Organization unit not found: " + user.getOrganizationUnit().getId())));
        }
        if (user.getLineManager() != null) {
            user.setLineManager(findById(user.getLineManager().getId()));
        }
    }

    private void assertUniqueIdentity(User candidate, Long currentUserId) {
        userRepository.findByUsername(candidate.getUsername())
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> { throw new DuplicateResourceException("Username already exists"); });
        userRepository.findByEmail(candidate.getEmail())
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> { throw new DuplicateResourceException("Email already exists"); });
        userRepository.findByResourceNumber(candidate.getResourceNumber())
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> { throw new DuplicateResourceException("Resource number already exists"); });
    }

    private void assertEncodedPassword(String passwordHash) {
        if (passwordHash == null || !(isDelegatingPasswordEncoderValue(passwordHash)
                || passwordHash.startsWith("$2a$")
                || passwordHash.startsWith("$2b$")
                || passwordHash.startsWith("$2y$")
                || passwordHash.startsWith("$argon2"))) {
            throw new InvalidOperationException("passwordHash must contain a supported encoded password, not plaintext");
        }
    }

    private boolean isDelegatingPasswordEncoderValue(String value) {
        int closingBrace = value.indexOf('}');
        return value.startsWith("{") && closingBrace > 1 && closingBrace < value.length() - 1;
    }

    private void validateManagerChain(User user, User candidateManager) {
        Set<Long> visited = new HashSet<>();
        User current = candidateManager;
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new InvalidOperationException("The line manager hierarchy already contains a cycle");
            }
            if (user.getId().equals(current.getId())) {
                throw new InvalidOperationException("Assigning this line manager would create a cycle");
            }
            current = current.getLineManager();
        }
    }
}
