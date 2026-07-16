package com.vyaparsetu.payment.service;

import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.payment.entity.CreditAccount;
import com.vyaparsetu.payment.repository.CreditAccountRepository;
import com.vyaparsetu.user.entity.Retailer;
import com.vyaparsetu.user.repository.RetailerRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Distributor-facing credit management: read and configure a retailer's credit line.
 * Resolves the current supplier and enforces that the retailer belongs to them.
 */
@Service
public class CreditAdminService {

    public record CreditView(Long retailerId, boolean approved, String status,
                             BigDecimal creditLimit, BigDecimal outstanding, BigDecimal available) {
    }

    public record SetLimitRequest(BigDecimal creditLimit, Boolean approved) {
    }

    public record StatusRequest(String status) {
    }

    private final CreditAccountRepository creditAccountRepository;
    private final CreditService creditService;
    private final RetailerRepository retailerRepository;
    private final UserService userService;

    public CreditAdminService(CreditAccountRepository creditAccountRepository, CreditService creditService,
                              RetailerRepository retailerRepository, UserService userService) {
        this.creditAccountRepository = creditAccountRepository;
        this.creditService = creditService;
        this.retailerRepository = retailerRepository;
        this.userService = userService;
    }

    private Retailer ownedRetailer(Long retailerId) {
        Long supplierId = userService.currentSupplierId();
        Retailer r = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));
        if (!supplierId.equals(r.getDistributorId())) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Not your retailer");
        }
        return r;
    }

    @Transactional(readOnly = true)
    public CreditView get(Long retailerId) {
        Retailer r = ownedRetailer(retailerId);
        Long supplierId = userService.currentSupplierId();
        BigDecimal outstanding = creditService.exposure(retailerId, supplierId);
        return creditAccountRepository.findByRetailerIdAndSupplierId(retailerId, supplierId)
                .map(a -> new CreditView(retailerId, r.isCreditApproved(), a.getStatus().name(),
                        a.getCreditLimit(), outstanding,
                        a.getCreditLimit().subtract(outstanding)))
                .orElse(new CreditView(retailerId, r.isCreditApproved(), "NONE",
                        BigDecimal.ZERO, outstanding, BigDecimal.ZERO.subtract(outstanding)));
    }

    @Transactional
    public CreditView setLimit(Long retailerId, SetLimitRequest req) {
        ownedRetailer(retailerId);
        Long supplierId = userService.currentSupplierId();
        if (req.creditLimit() != null) {
            creditService.setCreditLimit(retailerId, supplierId, req.creditLimit(), supplierId);
        }
        if (req.approved() != null) {
            creditService.approveRetailer(retailerId, req.approved());
        }
        return get(retailerId);
    }

    @Transactional
    public CreditView setStatus(Long retailerId, StatusRequest req) {
        ownedRetailer(retailerId);
        Long supplierId = userService.currentSupplierId();
        if (req == null || req.status() == null || req.status().isBlank()) {
            throw new BusinessException("INVALID_CREDIT_STATUS", HttpStatus.BAD_REQUEST,
                    "Credit status is required");
        }
        CreditAccount.Status status;
        try {
            status = CreditAccount.Status.valueOf(req.status().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_CREDIT_STATUS", HttpStatus.BAD_REQUEST,
                    "Credit status must be ACTIVE or SUSPENDED");
        }
        creditService.setStatus(retailerId, supplierId, status);
        return get(retailerId);
    }
}
