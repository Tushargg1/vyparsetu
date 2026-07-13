package com.vyaparsetu.dashboard;

import com.vyaparsetu.ai.service.ForecastService;
import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.inventory.dto.InventoryItemResponse;
import com.vyaparsetu.inventory.service.InventoryService;
import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.entity.OrderStatus;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.report.repository.ReportRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ReportRepository reportRepository;
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final ForecastService forecastService;
    private final ProductRepository productRepository;
    private final UserService userService;

    public DashboardService(ReportRepository reportRepository, InventoryService inventoryService,
                            OrderRepository orderRepository, ForecastService forecastService,
                            ProductRepository productRepository, UserService userService) {
        this.reportRepository = reportRepository;
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.forecastService = forecastService;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    public record LowStockItem(Long productId, String productName, BigDecimal quantity, BigDecimal reorderLevel) {
    }

    public record ActiveOrder(Long id, String orderNumber, String status, BigDecimal totalAmount) {
    }

    public record PendingPayment(Long orderId, String orderNumber, String paymentMode, BigDecimal amount) {
    }

    public record FutureOrder(Long productId, String productName, Integer daysToStockout,
                              LocalDate suggestedPurchaseDate, BigDecimal suggestedQty) {
    }

    public record RetailerDashboard(
            BigDecimal todaySales,
            BigDecimal todayProfit,
            List<LowStockItem> lowStock,
            List<ActiveOrder> activeOrders,
            BigDecimal pendingPaymentTotal,
            List<PendingPayment> pendingPayments,
            List<FutureOrder> futureOrders
    ) {
    }

    @Transactional(readOnly = true)
    public RetailerDashboard retailerDashboard() {
        Long rid = userService.currentRetailerId();
        Instant now = Instant.now();
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal todaySales = nz(reportRepository.retailerSalesValue(rid, startOfDay, now));
        BigDecimal todayCogs = nz(reportRepository.retailerCogs(rid, startOfDay, now));
        BigDecimal todayProfit = todaySales.subtract(todayCogs);

        // Low stock
        List<InventoryItemResponse> low = inventoryService.myLowStock();
        Map<Long, String> names = productNames(low.stream().map(InventoryItemResponse::productId).toList());
        List<LowStockItem> lowStock = low.stream()
                .map(i -> new LowStockItem(i.productId(), names.getOrDefault(i.productId(), "Product #" + i.productId()),
                        i.quantity(), i.reorderLevel()))
                .toList();

        // Active orders (in progress)
        List<ActiveOrder> active = orderRepository.findByRetailerIdAndStatusIn(rid, List.of(
                        OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PACKED, OrderStatus.OUT_FOR_DELIVERY))
                .stream()
                .map(o -> new ActiveOrder(o.getId(), o.getOrderNumber(), o.getStatus().name(), o.getTotalAmount()))
                .toList();

        // Pending payments to distributors (unpaid placed orders)
        List<Order> unpaid = orderRepository.findByRetailerIdAndPaymentStatus(rid, Enums.PaymentStatus.PENDING);
        List<PendingPayment> pending = unpaid.stream()
                .filter(o -> o.getStatus() != OrderStatus.DRAFT && o.getStatus() != OrderStatus.REJECTED
                        && o.getStatus() != OrderStatus.CANCELLED)
                .map(o -> new PendingPayment(o.getId(), o.getOrderNumber(), o.getPaymentMode().name(), o.getTotalAmount()))
                .toList();
        BigDecimal pendingTotal = pending.stream().map(PendingPayment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Future orders from AI demand forecast
        List<FutureOrder> future = forecastService.forecast().stream()
                .filter(f -> f.daysToStockout() != null)
                .map(f -> new FutureOrder(f.productId(),
                        productName(f.productId()), f.daysToStockout(), f.suggestedPurchaseDate(), f.suggestedQty()))
                .toList();

        return new RetailerDashboard(todaySales, todayProfit, lowStock, active, pendingTotal, pending, future);
    }

    private Map<Long, String> productNames(List<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        productRepository.findAllById(ids).forEach(p -> map.put(p.getId(), p.getName()));
        return map;
    }

    private String productName(Long id) {
        return productRepository.findById(id).map(Product::getName).orElse("Product #" + id);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
