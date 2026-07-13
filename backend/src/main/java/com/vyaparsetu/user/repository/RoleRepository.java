package com.vyaparsetu.user.repository;

import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
