package com.vyaparsetu.user.repository;

import com.vyaparsetu.user.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByUserId(Long userId);

    Optional<Supplier> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
