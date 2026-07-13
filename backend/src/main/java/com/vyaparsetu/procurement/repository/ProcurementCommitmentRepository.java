package com.vyaparsetu.procurement.repository;

import com.vyaparsetu.procurement.entity.ProcurementCommitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcurementCommitmentRepository extends JpaRepository<ProcurementCommitment, Long> {
    List<ProcurementCommitment> findByCampaignId(Long campaignId);

    List<ProcurementCommitment> findByRetailerId(Long retailerId);
}
