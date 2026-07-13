package com.vyaparsetu.payment.repository;

import com.vyaparsetu.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByUuid(String uuid);

    Optional<Payment> findByGatewayRef(String gatewayRef);

    List<Payment> findByOrderId(Long orderId);
}
