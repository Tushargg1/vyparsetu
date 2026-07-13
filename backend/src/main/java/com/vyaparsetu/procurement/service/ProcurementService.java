package com.vyaparsetu.procurement.service;

import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.procurement.entity.ProcurementCampaign;
import com.vyaparsetu.procurement.entity.ProcurementCommitment;
import com.vyaparsetu.procurement.repository.ProcurementCampaignRepository;
import com.vyaparsetu.procurement.repository.ProcurementCommitmentRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bulk procurement (Phase 3). Disabled by default via app.features.procurement.enabled.
 * Every public operation checks the flag so the module stays inert in V1.
 */
@Service
public class ProcurementService {

    private final ProcurementCampaignRepository campaignRepository;
    private final ProcurementCommitmentRepository commitmentRepository;
    private final UserService userService;
    private final AppProperties props;

    public ProcurementService(ProcurementCampaignRepository campaignRepository,
                              ProcurementCommitmentRepository commitmentRepository,
                              UserService userService, AppProperties props) {
        this.campaignRepository = campaignRepository;
        this.commitmentRepository = commitmentRepository;
        this.userService = userService;
        this.props = props;
    }

    private void ensureEnabled() {
        if (!props.getFeatures().getProcurement().isEnabled()) {
            throw new BusinessException("FEATURE_DISABLED", HttpStatus.FORBIDDEN,
                    "Bulk procurement is not enabled");
        }
    }

    @Transactional(readOnly = true)
    public List<ProcurementCampaign> openCampaigns() {
        ensureEnabled();
        return campaignRepository.findByStatus(ProcurementCampaign.Status.OPEN);
    }

    @Transactional
    public ProcurementCampaign createCampaign(Long productId, BigDecimal target, BigDecimal expectedPrice,
                                              java.time.Instant closesAt) {
        ensureEnabled();
        ProcurementCampaign c = new ProcurementCampaign();
        c.setProductId(productId);
        c.setTargetQuantity(target);
        c.setExpectedPrice(expectedPrice);
        c.setClosesAt(closesAt);
        return campaignRepository.save(c);
    }

    @Transactional
    public ProcurementCommitment join(Long campaignId, BigDecimal quantity, BigDecimal advancePaid) {
        ensureEnabled();
        ProcurementCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
        if (campaign.getStatus() != ProcurementCampaign.Status.OPEN) {
            throw new BusinessException("Campaign is not open");
        }
        Long retailerId = userService.currentRetailerId();

        ProcurementCommitment commitment = new ProcurementCommitment();
        commitment.setCampaignId(campaignId);
        commitment.setRetailerId(retailerId);
        commitment.setQuantity(quantity);
        commitment.setAdvancePaid(advancePaid);
        commitment = commitmentRepository.save(commitment);

        campaign.setCommittedQuantity(campaign.getCommittedQuantity().add(quantity));
        if (campaign.getCommittedQuantity().compareTo(campaign.getTargetQuantity()) >= 0) {
            campaign.setStatus(ProcurementCampaign.Status.CLOSED);
        }
        campaignRepository.save(campaign);
        return commitment;
    }
}
