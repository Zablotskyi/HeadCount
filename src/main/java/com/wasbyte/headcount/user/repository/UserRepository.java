package com.wasbyte.headcount.user.repository;

import com.wasbyte.headcount.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.Set;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @EntityGraph(attributePaths = {"roles", "organizationUnit", "lineManager"})
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = {"roles", "organizationUnit", "lineManager"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "organizationUnit", "lineManager"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "organizationUnit", "lineManager"})
    Optional<User> findByResourceNumber(String resourceNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByResourceNumber(String resourceNumber);

    long countDistinctByRolesName(String roleName);

    List<User> findByOrganizationUnitIdInAndEnabledTrue(Set<Long> organizationUnitIds);

    @Override
    @EntityGraph(attributePaths = {"roles", "organizationUnit", "lineManager"})
    List<User> findAll();

    @EntityGraph(attributePaths = {"roles", "organizationUnit", "lineManager"})
    List<User> findDistinctByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrResourceNumberContainingIgnoreCase(
            String username, String firstName, String lastName, String email, String resourceNumber);
}
