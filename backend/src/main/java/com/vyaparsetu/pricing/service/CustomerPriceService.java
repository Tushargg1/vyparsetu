package com.vyaparsetu.pricing.service;

import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.pricing.entity.CustomerPrice;
import com.vyaparsetu.pricing.repository.CustomerPriceRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Distributor-facing CRUD for customer-specific price overrides. */
@Service
public class CustomerPriceService {

    public record CustomerPriceView(Long id, Long retailerId, Long productId,
                                    BigDecimal unitPrice, boolean active) {
        static CustomerPriceView from(CustomerPrice p) {
            return new CustomerPriceView(p.getId(), p.getRetailerId(), p.getProductId(),
                    p.getUnitPrice(), p.isActive());
        }
    }

    public record UpsertRequest(Long retailerId, Long productId, BigDecimal unitPrice, Boolean active) {
    }

    private final CustomerPriceRepository repository;
    private final UserService userService;

    public CustomerPriceService(CustomerPriceRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<CustomerPriceView> listForRetailer(Long retailerId) {
        Long supplierId = userService.currentSupplierId();
        return repository.findBySupplierIdAndRetailerId(supplierId, retailerId).stream()
                .map(CustomerPriceView::from).toList();
    }

    @Transactional
    public CustomerPriceView upsert(UpsertRequest req) {
        Long supplierId = userService.currentSupplierId();
        CustomerPrice cp = repository
                .findBySupplierIdAndRetailerIdAndProductIdAndActiveTrue(
                        supplierId, req.retailerId(), req.productId())
                .orElseGet(() -> {
                    CustomerPrice n = new CustomerPrice();
                    n.setSupplierId(supplierId);
                    n.setRetailerId(req.retailerId());
                    n.setProductId(req.productId());
                    return n;
                });
        cp.setUnitPrice(req.unitPrice());
        if (req.active() != null) cp.setActive(req.active());
        return CustomerPriceView.from(repository.save(cp));
    }

    @Transactional
    public void delete(Long id) {
        Long supplierId = userService.currentSupplierId();
        CustomerPrice cp = repository.findById(id)
                .filter(p -> p.getSupplierId().equals(supplierId)) // SECURITY: own rows only
                .orElseThrow(() -> new ResourceNotFoundException("CustomerPrice", id));
        repository.delete(cp);
    }
}
