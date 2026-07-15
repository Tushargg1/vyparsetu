package com.vyaparsetu.payment.repository;

import com.vyaparsetu.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByUuid(String uuid);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p WHERE p.uuid = :uuid")
    Optional<Payment> findByUuidForUpdate(@org.springframework.data.repository.query.Param("uuid") String uuid);

    Optional<Payment> findByGatewayRef(String gatewayRef);

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
            Long orderId, java.util.Collection<Payment.PaymentTxnStatus> statuses);
}
