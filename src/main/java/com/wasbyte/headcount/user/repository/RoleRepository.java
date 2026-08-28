package com.wasbyte.headcount.user.repository;

import com.wasbyte.headcount.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    List<Role> findAllByOrderByNameAsc();
}
