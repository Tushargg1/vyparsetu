package com.vyaparsetu.whatsapp.repository;

import com.vyaparsetu.whatsapp.entity.WhatsAppSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WhatsAppSessionRepository extends JpaRepository<WhatsAppSession, Long> {
    Optional<WhatsAppSession> findBySupplierIdAndPhone(Long supplierId, String phone);
}
