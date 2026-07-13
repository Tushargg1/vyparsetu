package com.vyaparsetu.procurement.repository;

import com.vyaparsetu.procurement.entity.ProcurementCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcurementCampaignRepository extends JpaRepository<ProcurementCampaign, Long> {
    List<ProcurementCampaign> findByStatus(ProcurementCampaign.Status status);
}
