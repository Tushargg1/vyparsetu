package com.vyaparsetu.payment.repository;

import com.vyaparsetu.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
