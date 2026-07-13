package com.vyaparsetu.whatsapp.repository;

import com.vyaparsetu.whatsapp.WhatsAppEnums;
import com.vyaparsetu.whatsapp.entity.RetailerRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetailerRequestRepository extends JpaRepository<RetailerRequest, Long> {
    List<RetailerRequest> findBySupplierIdOrderByIdDesc(Long supplierId);

    List<RetailerRequest> findBySupplierIdAndStatusOrderByIdDesc(Long supplierId, WhatsAppEnums.RequestStatus status);

    Optional<RetailerRequest> findBySupplierIdAndPhoneAndStatus(Long supplierId, String phone, WhatsAppEnums.RequestStatus status);
}
