package com.vyaparsetu.whatsapp.repository;

import com.vyaparsetu.whatsapp.entity.RetailerPhone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetailerPhoneRepository extends JpaRepository<RetailerPhone, Long> {
    List<RetailerPhone> findByRetailerId(Long retailerId);

    Optional<RetailerPhone> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
