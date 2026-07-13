package com.vyaparsetu.payment.repository;

import com.vyaparsetu.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByRetailerId(Long retailerId);
}
