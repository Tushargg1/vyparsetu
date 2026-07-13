package com.vyaparsetu.whatsapp.repository;

import com.vyaparsetu.whatsapp.entity.WhatsAppPendingOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WhatsAppPendingOrderRepository extends JpaRepository<WhatsAppPendingOrder, Long> {
    Optional<WhatsAppPendingOrder> findByPhone(String phone);

    void deleteByPhone(String phone);
}
