package com.vyaparsetu.user.service;

import com.vyaparsetu.user.entity.DistributorPolicy;
import com.vyaparsetu.user.repository.DistributorPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Reads/writes a distributor's ordering policies, creating defaults on demand. */
@Service
public class PolicyService {

    public record PolicyView(
            String outOfStockBehavior,
            BigDecimal minOrderValue,
            BigDecimal minOrderQty,
            int autoCancelHours,
            boolean enforceCreditLimit,
            String creditOverLimitAction,
            BigDecimal largeOrderThreshold,
            boolean allowOrderingWithoutApproval
    ) {
        static PolicyView from(DistributorPolicy p) {
            return new PolicyView(p.getOutOfStockBehavior().name(), p.getMinOrderValue(),
                    p.getMinOrderQty(), p.getAutoCancelHours(), p.isEnforceCreditLimit(),
                    p.getCreditOverLimitAction().name(), p.getLargeOrderThreshold(),
                    p.isAllowOrderingWithoutApproval());
        }
    }

    public record PolicyUpdate(
            String outOfStockBehavior,
            BigDecimal minOrderValue,
            BigDecimal minOrderQty,
            Integer autoCancelHours,
            Boolean enforceCreditLimit,
            String creditOverLimitAction,
            BigDecimal largeOrderThreshold,
            Boolean allowOrderingWithoutApproval
    ) {
    }

    private final DistributorPolicyRepository repo;
    private final UserService userService;

    public PolicyService(DistributorPolicyRepository repo, UserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    @Transactional
    public DistributorPolicy policyFor(Long supplierId) {
        return repo.findBySupplierId(supplierId).orElseGet(() -> {
            DistributorPolicy p = new DistributorPolicy();
            p.setSupplierId(supplierId);
            return repo.save(p);
        });
    }

    @Transactional(readOnly = true)
    public PolicyView get() {
        return PolicyView.from(policyFor(userService.currentSupplierId()));
    }

    @Transactional
    public PolicyView update(PolicyUpdate req) {
        DistributorPolicy p = policyFor(userService.currentSupplierId());
        if (req.outOfStockBehavior() != null) {
            p.setOutOfStockBehavior(DistributorPolicy.OutOfStockBehavior.valueOf(req.outOfStockBehavior()));
        }
        if (req.minOrderValue() != null) p.setMinOrderValue(req.minOrderValue());
        if (req.minOrderQty() != null) p.setMinOrderQty(req.minOrderQty());
        if (req.autoCancelHours() != null) p.setAutoCancelHours(req.autoCancelHours());
        if (req.enforceCreditLimit() != null) p.setEnforceCreditLimit(req.enforceCreditLimit());
        if (req.creditOverLimitAction() != null) {
            p.setCreditOverLimitAction(
                    DistributorPolicy.CreditOverLimitAction.valueOf(req.creditOverLimitAction()));
        }
        if (req.largeOrderThreshold() != null) p.setLargeOrderThreshold(req.largeOrderThreshold());
        if (req.allowOrderingWithoutApproval() != null) {
            p.setAllowOrderingWithoutApproval(req.allowOrderingWithoutApproval());
        }
        return PolicyView.from(repo.save(p));
    }
}
