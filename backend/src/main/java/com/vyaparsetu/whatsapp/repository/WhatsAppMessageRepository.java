package com.vyaparsetu.whatsapp.repository;

import com.vyaparsetu.whatsapp.entity.WhatsAppMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    boolean existsByProviderMessageId(String providerMessageId);

    List<WhatsAppMessage> findBySupplierIdAndPhoneOrderByIdDesc(Long supplierId, String phone, Pageable pageable);
}
