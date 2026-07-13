package com.vyaparsetu.payment.repository;

import com.vyaparsetu.payment.entity.CreditAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {
    Optional<CreditAccount> findByRetailerIdAndSupplierId(Long retailerId, Long supplierId);
}
