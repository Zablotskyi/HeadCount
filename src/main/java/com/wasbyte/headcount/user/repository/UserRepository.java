package com.wasbyte.headcount.user.repository;

import com.wasbyte.headcount.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByResourceNumber(String resourceNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByResourceNumber(String resourceNumber);

    List<User> findByOrganizationUnitIdInAndEnabledTrue(Set<Long> organizationUnitIds);
}
