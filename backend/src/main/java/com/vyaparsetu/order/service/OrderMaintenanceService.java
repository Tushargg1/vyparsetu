package com.vyaparsetu.order.service;

import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.entity.OrderStatus;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.user.entity.DistributorPolicy;
import com.vyaparsetu.user.repository.DistributorPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Background maintenance: auto-cancels stale PENDING orders according to each
 * distributor's {@code autoCancelHours} policy, releasing their reserved stock.
 */
@Service
public class OrderMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(OrderMaintenanceService.class);

    private final OrderRepository orderRepository;
    private final DistributorPolicyRepository policyRepository;
    private final OrderService orderService;

    public OrderMaintenanceService(OrderRepository orderRepository,
                                   DistributorPolicyRepository policyRepository,
                                   OrderService orderService) {
        this.orderRepository = orderRepository;
        this.policyRepository = policyRepository;
        this.orderService = orderService;
    }

    /** Runs every 15 minutes (and 1 minute after startup). */
    @Scheduled(fixedDelay = 15 * 60 * 1000, initialDelay = 60 * 1000)
    public void autoCancelStalePendingOrders() {
        // Only suppliers that enabled auto-cancel.
        Map<Long, Integer> hoursBySupplier = new HashMap<>();
        for (DistributorPolicy p : policyRepository.findAll()) {
            if (p.getAutoCancelHours() > 0) {
                hoursBySupplier.put(p.getSupplierId(), p.getAutoCancelHours());
            }
        }
        if (hoursBySupplier.isEmpty()) return;

        // Oldest threshold across suppliers limits the candidate scan.
        int maxHours = hoursBySupplier.values().stream().max(Integer::compareTo).orElse(0);
        Instant cutoff = Instant.now().minus(Duration.ofHours(1)); // any order older than 1h is worth checking
        List<Order> candidates = orderRepository.findByStatusAndPlacedAtBefore(OrderStatus.PENDING, cutoff);

        Instant now = Instant.now();
        int cancelled = 0;
        for (Order o : candidates) {
            Integer hours = hoursBySupplier.get(o.getSupplierId());
            if (hours == null || o.getPlacedAt() == null) continue;
            if (o.getPlacedAt().isBefore(now.minus(Duration.ofHours(hours)))) {
                try {
                    orderService.systemCancel(o.getId(),
                            "Auto-cancelled: not accepted within " + hours + "h");
                    cancelled++;
                } catch (RuntimeException e) {
                    log.warn("[AUTO-CANCEL] order {} failed: {}", o.getId(), e.getMessage());
                }
            }
        }
        if (cancelled > 0) log.info("[AUTO-CANCEL] cancelled {} stale pending order(s) (maxHours={})", cancelled, maxHours);
    }
}
