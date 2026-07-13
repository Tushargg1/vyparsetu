package com.vyaparsetu.user.bootstrap;

import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.user.entity.Role;
import com.vyaparsetu.user.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures the four base roles exist. Idempotent, so it is safe whether or not
 * the Flyway seed already inserted them (e.g. the in-memory local profile).
 */
@Component
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        for (RoleName name : RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                Role role = new Role();
                role.setName(name);
                roleRepository.save(role);
            }
        }
    }
}
