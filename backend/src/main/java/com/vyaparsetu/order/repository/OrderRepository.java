package com.vyaparsetu.order.repository;

import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUuid(String uuid);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Page<Order> findByRetailerId(Long retailerId, Pageable pageable);

    Page<Order> findBySupplierId(Long supplierId, Pageable pageable);

    Page<Order> findBySupplierIdAndStatus(Long supplierId, OrderStatus status, Pageable pageable);

    java.util.List<Order> findByStatusAndPlacedAtBefore(OrderStatus status, java.time.Instant before);

    Optional<Order> findTopByRetailerIdAndStatusNotOrderByCreatedAtDesc(Long retailerId, OrderStatus status);

    Optional<Order> findTopByRetailerIdOrderByIdDesc(Long retailerId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o.retailerId FROM Order o WHERE o.supplierId = :supplierId")
    java.util.List<Long> findDistinctRetailerIdsBySupplierId(@org.springframework.data.repository.query.Param("supplierId") Long supplierId);

    java.util.List<Order> findByRetailerIdAndPlacedAtBetween(Long retailerId, java.time.Instant from, java.time.Instant to);

    java.util.List<Order> findBySupplierIdAndPlacedAtBetween(Long supplierId, java.time.Instant from, java.time.Instant to);

    java.util.List<Order> findByRetailerIdAndStatusIn(Long retailerId, java.util.Collection<OrderStatus> statuses);

    java.util.List<Order> findByRetailerIdAndPaymentStatus(Long retailerId, com.vyaparsetu.common.enums.Enums.PaymentStatus paymentStatus);

    java.util.List<Order> findByRetailerIdAndSupplierIdOrderByPlacedAtAsc(Long retailerId, Long supplierId);
}
