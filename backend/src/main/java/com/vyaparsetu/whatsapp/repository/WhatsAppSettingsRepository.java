package com.vyaparsetu.whatsapp.repository;

import com.vyaparsetu.whatsapp.entity.WhatsAppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WhatsAppSettingsRepository extends JpaRepository<WhatsAppSettings, Long> {
    Optional<WhatsAppSettings> findBySupplierId(Long supplierId);

    Optional<WhatsAppSettings> findByBusinessNumberAndConnectedTrue(String businessNumber);
}
