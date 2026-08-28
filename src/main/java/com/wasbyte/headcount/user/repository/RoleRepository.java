package com.wasbyte.headcount.user.repository;

import com.wasbyte.headcount.user.entity.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select role from Role role where role.name = :name")
    Optional<Role> findByNameForUpdate(@Param("name") String name);

    List<Role> findAllByOrderByNameAsc();
}
