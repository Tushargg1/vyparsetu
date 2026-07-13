package com.vyaparsetu.catalog.service;

import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.notification.service.NotificationService;
import com.vyaparsetu.user.entity.DistributorPolicy;
import com.vyaparsetu.user.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Distributor stock engine: availability checks against the out-of-stock policy,
 * plus reserve / release / decrement tied to the order lifecycle. Stock is on-hand
 * minus reserved; reservations are held from order creation until delivery or cancel.
 */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final SupplierRepository supplierRepository;

    public StockService(ProductRepository productRepository, NotificationService notificationService,
                        SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.supplierRepository = supplierRepository;
    }

    public record Line(Long productId, String name, BigDecimal qty) {
    }

    public record Result(boolean ok, List<Line> lines, List<String> messages) {
    }

    /**
     * Validate requested lines against available stock and the distributor's
     * out-of-stock policy. Returns possibly-adjusted lines (PARTIAL) or ok=false
     * (REJECT with shortfalls). BACKORDER passes everything through.
     */
    @Transactional(readOnly = true)
    public Result validate(List<Line> requested, DistributorPolicy.OutOfStockBehavior behavior) {
        List<Line> out = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        boolean ok = true;

        for (Line l : requested) {
            Product p = productRepository.findById(l.productId()).orElse(null);
            if (p == null) continue;
            if (!p.isTrackStock()) {
                out.add(l);
                continue;
            }
            BigDecimal available = p.getAvailableQty();
            if (l.qty().compareTo(available) <= 0) {
                out.add(l);
                continue;
            }
            // shortfall
            switch (behavior) {
                case BACKORDER -> {
                    out.add(l);
                    messages.add(p.getName() + ": " + trim(available) + " in stock, " + trim(l.qty())
                            + " ordered — remainder backordered.");
                }
                case PARTIAL -> {
                    if (available.signum() > 0) {
                        out.add(new Line(p.getId(), p.getName(), available));
                        messages.add(p.getName() + ": only " + trim(available) + " available — quantity adjusted.");
                    } else {
                        messages.add(p.getName() + ": out of stock — removed.");
                    }
                }
                default -> { // REJECT
                    ok = false;
                    messages.add(p.getName() + ": only " + trim(available) + " in stock (you asked for "
                            + trim(l.qty()) + ").");
                }
            }
        }
        return new Result(ok, out, messages);
    }

    @Transactional
    public void reserve(List<Line> lines) {
        for (Line l : lines) {
            Product p = productRepository.findById(l.productId()).orElse(null);
            if (p == null || !p.isTrackStock()) continue;
            p.setReservedQty(nz(p.getReservedQty()).add(l.qty()));
            productRepository.save(p);
            maybeNotifyLowStock(p);
        }
    }

    @Transactional
    public void release(List<Line> lines) {
        for (Line l : lines) {
            Product p = productRepository.findById(l.productId()).orElse(null);
            if (p == null || !p.isTrackStock()) continue;
            p.setReservedQty(floorZero(nz(p.getReservedQty()).subtract(l.qty())));
            productRepository.save(p);
        }
    }

    /** Fulfilment: remove from on-hand and release the held reservation. */
    @Transactional
    public void decrement(List<Line> lines) {
        for (Line l : lines) {
            Product p = productRepository.findById(l.productId()).orElse(null);
            if (p == null || !p.isTrackStock()) continue;
            p.setStockQty(floorZero(nz(p.getStockQty()).subtract(l.qty())));
            p.setReservedQty(floorZero(nz(p.getReservedQty()).subtract(l.qty())));
            productRepository.save(p);
            maybeNotifyLowStock(p);
        }
    }

    private void maybeNotifyLowStock(Product p) {
        if (!p.isTrackStock()) return;
        if (p.getAvailableQty().compareTo(nz(p.getLowStockThreshold())) <= 0) {
            supplierRepository.findById(p.getSupplierId()).ifPresent(s -> {
                try {
                    notificationService.notify(s.getUserId(), Enums.NotificationType.LOW_STOCK,
                            "Low stock: " + p.getName(),
                            p.getName() + " is low (" + trim(p.getAvailableQty()) + " available).");
                } catch (Exception e) {
                    log.warn("[STOCK] low-stock notify failed: {}", e.getMessage());
                }
            });
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal floorZero(BigDecimal v) {
        return v.signum() < 0 ? BigDecimal.ZERO : v;
    }

    private String trim(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
