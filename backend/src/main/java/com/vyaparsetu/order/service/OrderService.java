package com.vyaparsetu.order.service;

import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.catalog.service.StockService;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.CreditLimitExceededException;
import com.vyaparsetu.common.exception.InvalidOrderStateException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.common.response.PageResponse;
import com.vyaparsetu.common.security.CurrentUser;
import com.vyaparsetu.common.util.NumberGenerator;
import com.vyaparsetu.analytics.service.AnalyticsService;
import com.vyaparsetu.inventory.service.InventoryService;
import com.vyaparsetu.notification.service.NotificationService;
import com.vyaparsetu.order.dto.OrderResponse;
import com.vyaparsetu.order.dto.PlaceOrderRequest;
import com.vyaparsetu.order.entity.*;
import com.vyaparsetu.order.repository.*;
import com.vyaparsetu.payment.service.CreditService;
import com.vyaparsetu.pricing.service.PricingService;
import com.vyaparsetu.user.entity.DistributorPolicy;
import com.vyaparsetu.user.repository.RetailerRepository;
import com.vyaparsetu.user.repository.SupplierRepository;
import com.vyaparsetu.user.service.PolicyService;
import com.vyaparsetu.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    /** Which roles may drive each target transition (Property 4 actor authorization). */
    private static final Map<OrderStatus, Set<String>> TRANSITION_ROLES = new EnumMap<>(OrderStatus.class);
    static {
        TRANSITION_ROLES.put(OrderStatus.ACCEPTED, Set.of("SUPPLIER", "ADMIN"));
        TRANSITION_ROLES.put(OrderStatus.REJECTED, Set.of("SUPPLIER", "ADMIN"));
        TRANSITION_ROLES.put(OrderStatus.PACKED, Set.of("SUPPLIER", "ADMIN"));
        TRANSITION_ROLES.put(OrderStatus.OUT_FOR_DELIVERY, Set.of("SUPPLIER", "ADMIN"));
        TRANSITION_ROLES.put(OrderStatus.DELIVERED, Set.of("SUPPLIER", "ADMIN"));
        TRANSITION_ROLES.put(OrderStatus.CASH_COLLECTED, Set.of("SUPPLIER", "ADMIN"));
        TRANSITION_ROLES.put(OrderStatus.COMPLETED, Set.of("RETAILER", "SUPPLIER", "ADMIN"));
        TRANSITION_ROLES.put(OrderStatus.CANCELLED, Set.of("RETAILER", "SUPPLIER", "ADMIN"));
        TRANSITION_ROLES.put(OrderStatus.RETURNED, Set.of("RETAILER", "SUPPLIER", "ADMIN"));
    }

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final RetailerRepository retailerRepository;
    private final SupplierRepository supplierRepository;
    private final com.vyaparsetu.catalog.service.StockService stockService;
    private final PricingService pricingService;
    private final CreditService creditService;
    private final PolicyService policyService;
    private final AnalyticsService analyticsService;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        OrderStatusHistoryRepository historyRepository, CartRepository cartRepository,
                        CartItemRepository cartItemRepository, ProductRepository productRepository,
                        InventoryService inventoryService, UserService userService,
                        NotificationService notificationService, RetailerRepository retailerRepository,
                        SupplierRepository supplierRepository,
                        com.vyaparsetu.catalog.service.StockService stockService,
                        PricingService pricingService, CreditService creditService,
                        PolicyService policyService, AnalyticsService analyticsService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.historyRepository = historyRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.retailerRepository = retailerRepository;
        this.supplierRepository = supplierRepository;
        this.stockService = stockService;
        this.pricingService = pricingService;
        this.creditService = creditService;
        this.policyService = policyService;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req) {
        Long retailerId = userService.currentRetailerId();
        Cart cart = cartRepository.findByRetailerIdAndSupplierId(retailerId, req.supplierId())
                .orElseThrow(() -> new BusinessException("Cart is empty"));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }
        List<LineItem> lineItems = cartItems.stream()
                .map(ci -> new LineItem(ci.getProductId(), ci.getQuantity()))
                .toList();

        Order order = createOrderForRetailer(retailerId, req.supplierId(), lineItems,
                req.orderSource() != null ? req.orderSource() : Enums.OrderSource.CART,
                req.paymentMode(), req.deliveryAddressId(), userService != null ? safeCurrentUserId() : null);

        // clear cart
        cartItemRepository.deleteByCartId(cart.getId());
        return toResponse(order);
    }

    /** Item line used when building an order programmatically (cart or WhatsApp). */
    public record LineItem(Long productId, BigDecimal quantity) {
    }

    /**
     * Builds and persists a PENDING order for a retailer from explicit line items.
     * Used by both the cart flow and the WhatsApp webhook (which has no security context).
     * Validates that every product belongs to the target distributor.
     */
    @Transactional
    public Order createOrderForRetailer(Long retailerId, Long supplierId, List<LineItem> lineItems,
                                        Enums.OrderSource source, Enums.PaymentMode paymentMode,
                                        Long deliveryAddressId, Long actorUserId) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new BusinessException("Order has no items");
        }
        Map<Long, Product> products = productRepository.findAllById(
                        lineItems.stream().map(LineItem::productId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        // STOCK POLICY enforcement for EVERY order path (cart, WhatsApp, repeat).
        // The WhatsApp flow pre-validates for nicer messaging; this is the authoritative gate.
        DistributorPolicy policy = policyService.policyFor(supplierId);
        List<StockService.Line> requestedLines = lineItems.stream()
                .filter(li -> products.containsKey(li.productId()))
                .map(li -> new StockService.Line(li.productId(),
                        products.get(li.productId()).getName(), li.quantity()))
                .toList();
        StockService.Result stockCheck = stockService.validate(requestedLines, policy.getOutOfStockBehavior());
        if (!stockCheck.ok()) {
            throw new BusinessException("INSUFFICIENT_STOCK", org.springframework.http.HttpStatus.CONFLICT,
                    String.join(" ", stockCheck.messages()));
        }
        // PARTIAL may reduce/drop lines; the validated set becomes the effective order.
        List<LineItem> effectiveLines = stockCheck.lines().stream()
                .map(l -> new LineItem(l.productId(), l.qty()))
                .toList();
        if (effectiveLines.isEmpty()) {
            throw new BusinessException("OUT_OF_STOCK", org.springframework.http.HttpStatus.CONFLICT,
                    "All requested items are out of stock");
        }

        Order order = new Order();
        order.setOrderNumber(NumberGenerator.orderNumber());
        order.setRetailerId(retailerId);
        order.setSupplierId(supplierId);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderSource(source);
        order.setPaymentMode(paymentMode);
        order.setPaymentStatus(Enums.PaymentStatus.PENDING);
        order.setDeliveryAddressId(deliveryAddressId);
        order.setPlacedAt(Instant.now());
        order = orderRepository.save(order);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (LineItem li : effectiveLines) {
            Product p = products.get(li.productId());
            if (p == null) continue;
            // ISOLATION: never let another distributor's product enter the order.
            if (!p.getSupplierId().equals(supplierId)) {
                throw new BusinessException("PRODUCT_SUPPLIER_MISMATCH", org.springframework.http.HttpStatus.BAD_REQUEST,
                        "A product does not belong to this distributor");
            }
            // PRICE ENGINE: resolve unit price (customer-specific override -> default) in the backend only.
            PricingService.PricedLine priced = pricingService.priceLine(p, retailerId, li.quantity());

            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setQuantity(li.quantity());
            oi.setUnitPrice(priced.unitPrice());
            oi.setGstRate(priced.gstRate());
            oi.setLineTotal(priced.lineTotal());
            orderItemRepository.save(oi);

            subtotal = subtotal.add(priced.lineTotal());
            tax = tax.add(priced.lineTax());
        }
        order.setSubtotal(subtotal);
        order.setTaxAmount(tax);
        order.setDiscountAmount(BigDecimal.ZERO);
        BigDecimal grandTotal = subtotal.add(tax);
        order.setTotalAmount(grandTotal);

        // CREDIT ENFORCEMENT (Property 5): apply distributor policy before persisting totals.
        if (policy.isEnforceCreditLimit()) {
            CreditService.CreditCheck check = creditService.evaluate(retailerId, supplierId, grandTotal);
            if (check.hasAccount() && !check.withinLimit()) {
                notifyDistributor(supplierId, Enums.NotificationType.CREDIT_EXCEEDED,
                        "Credit limit hit on order " + order.getOrderNumber(),
                        "Order ₹" + grandTotal + " exceeds available credit ₹" + check.available());
                if (policy.getCreditOverLimitAction() == DistributorPolicy.CreditOverLimitAction.BLOCK) {
                    throw new CreditLimitExceededException(
                            "Order blocked: amount ₹" + grandTotal + " exceeds available credit ₹"
                                    + check.available());
                }
                // REQUIRE_APPROVAL: keep order in PENDING for the distributor to accept manually.
            }
        }
        order = orderRepository.save(order);

        recordHistory(order.getId(), null, OrderStatus.PENDING, "Order placed", actorUserId);

        // Reserve distributor stock for tracked products (held until delivery/cancel).
        List<StockService.Line> stockLines = effectiveLines.stream()
                .filter(li -> products.containsKey(li.productId()))
                .map(li -> new StockService.Line(li.productId(),
                        products.get(li.productId()).getName(), li.quantity()))
                .toList();
        stockService.reserve(stockLines);

        // ANALYTICS: best-effort metric capture (never blocks ordering).
        final Order placed = order;
        try {
            analyticsService.recordOrderPlaced(supplierId, retailerId, grandTotal);
            for (LineItem li : effectiveLines) {
                if (products.containsKey(li.productId())) {
                    analyticsService.recordProductOrdered(supplierId, retailerId, li.productId(), li.quantity());
                }
            }
        } catch (RuntimeException ignored) {
            // analytics must never break order creation
        }

        notifyDistributor(placed.getSupplierId(), Enums.NotificationType.ORDER_UPDATE,
                "New order " + placed.getOrderNumber(),
                "A retailer placed an order worth ₹" + placed.getTotalAmount());

        // LARGE ORDER alert when configured.
        if (policy.getLargeOrderThreshold().signum() > 0
                && grandTotal.compareTo(policy.getLargeOrderThreshold()) >= 0) {
            notifyDistributor(placed.getSupplierId(), Enums.NotificationType.LARGE_ORDER,
                    "Large order " + placed.getOrderNumber(),
                    "Order value ₹" + grandTotal + " is at or above your large-order threshold");
        }

        return order;
    }

    /** Sends a notification to the distributor's user account, if resolvable. */
    private void notifyDistributor(Long supplierId, Enums.NotificationType type, String title, String body) {
        supplierRepository.findById(supplierId).ifPresent(s ->
                notificationService.notify(s.getUserId(), type, title, body));
    }

    /** Statuses during which a retailer may still change the order contents. */
    private static final Set<OrderStatus> MODIFIABLE = Set.of(
            OrderStatus.DRAFT, OrderStatus.PENDING, OrderStatus.ACCEPTED);

    /**
     * Replaces the line items of an existing order (add/remove products, change quantities).
     * Allowed only before packing begins; afterwards modification is rejected.
     * Re-reserves stock, re-prices through the price engine and recomputes totals.
     */
    @Transactional
    public OrderResponse modifyOrder(Long id, List<LineItem> newLineItems) {
        if (newLineItems == null || newLineItems.isEmpty()) {
            throw new BusinessException("Order must keep at least one item");
        }
        if (newLineItems.stream().anyMatch(line -> line.productId() == null
                || line.quantity() == null || line.quantity().signum() <= 0)) {
            throw new BusinessException("INVALID_ORDER_ITEMS", org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Every order item needs a valid product and a quantity greater than zero");
        }
        Order order = find(id);
        ensureParticipant(order);
        if (!MODIFIABLE.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Order can no longer be modified (status " + order.getStatus() + ")");
        }

        Map<Long, Product> products = productRepository.findAllById(
                        newLineItems.stream().map(LineItem::productId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        long requestedProductCount = newLineItems.stream().map(LineItem::productId).distinct().count();
        if (products.size() != requestedProductCount) {
            throw new BusinessException("PRODUCT_NOT_FOUND", org.springframework.http.HttpStatus.BAD_REQUEST,
                    "One or more requested products do not exist");
        }

        // Release the previously reserved stock, then re-reserve against the new lines.
        stockService.release(stockLinesOf(order));
        orderItemRepository.deleteByOrderId(order.getId());

        // STOCK POLICY enforcement on the modified lines (freed reservation already released above).
        DistributorPolicy policy = policyService.policyFor(order.getSupplierId());
        List<StockService.Line> requested = newLineItems.stream()
                .filter(li -> products.containsKey(li.productId()))
                .map(li -> new StockService.Line(li.productId(),
                        products.get(li.productId()).getName(), li.quantity()))
                .toList();
        StockService.Result stockCheck = stockService.validate(requested, policy.getOutOfStockBehavior());
        if (!stockCheck.ok()) {
            throw new BusinessException("INSUFFICIENT_STOCK", org.springframework.http.HttpStatus.CONFLICT,
                    String.join(" ", stockCheck.messages()));
        }
        List<LineItem> effectiveLines = stockCheck.lines().stream()
                .map(l -> new LineItem(l.productId(), l.qty()))
                .toList();
        if (effectiveLines.isEmpty()) {
            throw new BusinessException("OUT_OF_STOCK", org.springframework.http.HttpStatus.CONFLICT,
                    "All requested items are out of stock");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (LineItem li : effectiveLines) {
            Product p = products.get(li.productId());
            if (p == null) continue;
            if (!p.getSupplierId().equals(order.getSupplierId())) {
                throw new BusinessException("PRODUCT_SUPPLIER_MISMATCH",
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "A product does not belong to this distributor");
            }
            PricingService.PricedLine priced = pricingService.priceLine(p, order.getRetailerId(), li.quantity());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setQuantity(li.quantity());
            oi.setUnitPrice(priced.unitPrice());
            oi.setGstRate(priced.gstRate());
            oi.setLineTotal(priced.lineTotal());
            orderItemRepository.save(oi);
            subtotal = subtotal.add(priced.lineTotal());
            tax = tax.add(priced.lineTax());
        }
        order.setSubtotal(subtotal);
        order.setTaxAmount(tax);
        order.setTotalAmount(subtotal.add(tax));
        orderRepository.save(order);

        List<StockService.Line> stockLines = effectiveLines.stream()
                .filter(li -> products.containsKey(li.productId()))
                .map(li -> new StockService.Line(li.productId(),
                        products.get(li.productId()).getName(), li.quantity()))
                .toList();
        stockService.reserve(stockLines);

        recordHistory(order.getId(), order.getStatus(), order.getStatus(),
                "Order items modified", safeCurrentUserId());
        try {
            analyticsService.record(order.getSupplierId(), order.getRetailerId(),
                    com.vyaparsetu.analytics.entity.AnalyticsEvent.EventType.ORDER_MODIFIED,
                    null, order.getTotalAmount(), null);
        } catch (RuntimeException ignored) {
        }
        notifyDistributor(order.getSupplierId(), Enums.NotificationType.ORDER_MODIFIED,
                "Order " + order.getOrderNumber() + " modified",
                "Items changed; new total ₹" + order.getTotalAmount());

        return toResponse(order);
    }

    private Long safeCurrentUserId() {
        try {
            return CurrentUser.id();
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> myOrders(int page, int size) {
        Long retailerId = userService.currentRetailerId();
        Page<Order> orders = orderRepository.findByRetailerId(retailerId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PageResponse.from(orders.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> supplierOrders(OrderStatus status, int page, int size) {
        Long supplierId = userService.currentSupplierId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = (status == null)
                ? orderRepository.findBySupplierId(supplierId, pageable)
                : orderRepository.findBySupplierIdAndStatus(supplierId, status, pageable);
        return PageResponse.from(orders.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = find(id);
        ensureParticipant(order);
        return toResponse(order);
    }

    /** Status-change timeline for an order (audit trail). */
    public record HistoryEntry(String fromStatus, String toStatus, Long changedBy,
                               String note, Instant at) {
    }

    @Transactional(readOnly = true)
    public List<HistoryEntry> history(Long id) {
        Order order = find(id);
        ensureParticipant(order);
        return historyRepository.findByOrderIdOrderByCreatedAtAsc(id).stream()
                .map(h -> new HistoryEntry(h.getFromStatus(), h.getToStatus(),
                        h.getChangedBy(), h.getNote(), h.getCreatedAt()))
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus target, String note) {        Order order = find(id);
        ensureParticipant(order);
        authorizeTransition(order, target);
        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidOrderStateException("Cannot move order from " + current + " to " + target);
        }
        order.setStatus(target);
        Instant nowTs = Instant.now();
        if (target == OrderStatus.ACCEPTED && order.getAcceptedAt() == null) {
            order.setAcceptedAt(nowTs);
        } else if (target == OrderStatus.PACKED && order.getPackedAt() == null) {
            order.setPackedAt(nowTs);
        } else if (target == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
            order.setDeliveredAt(nowTs);
        }
        if (target == OrderStatus.DELIVERED) {
            addDeliveredStockToRetailer(order);
            stockService.decrement(stockLinesOf(order));
        } else if (target == OrderStatus.REJECTED || target == OrderStatus.CANCELLED) {
            stockService.release(stockLinesOf(order));
        }
        orderRepository.save(order);
        recordHistory(order.getId(), current, target, note, safeCurrentUserId());

        // notify the retailer of the status change
        retailerRepository.findById(order.getRetailerId()).ifPresent(r ->
                notificationService.notify(r.getUserId(), Enums.NotificationType.ORDER_UPDATE,
                        "Order " + order.getOrderNumber() + " " + target,
                        "Your order status is now " + target));

        return toResponse(order);
    }

    @Transactional
    public OrderResponse repeatLast() {
        Long retailerId = userService.currentRetailerId();
        Order last = orderRepository
                .findTopByRetailerIdAndStatusNotOrderByCreatedAtDesc(retailerId, OrderStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException("No previous order to repeat", retailerId));
        List<OrderItem> items = orderItemRepository.findByOrderId(last.getId());

        Cart cart = cartRepository.findByRetailerIdAndSupplierId(retailerId, last.getSupplierId())
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setRetailerId(retailerId);
                    c.setSupplierId(last.getSupplierId());
                    return cartRepository.save(c);
                });
        for (OrderItem oi : items) {
            CartItem ci = cartItemRepository.findByCartIdAndProductId(cart.getId(), oi.getProductId())
                    .orElseGet(() -> {
                        CartItem n = new CartItem();
                        n.setCartId(cart.getId());
                        n.setProductId(oi.getProductId());
                        return n;
                    });
            ci.setQuantity(oi.getQuantity());
            cartItemRepository.save(ci);
        }
        // return the source order as reference
        return toResponse(last);
    }

    /**
     * System-initiated cancellation (no security context) used by the auto-cancel job.
     * Releases reserved stock, records history with no actor, and notifies the retailer.
     */
    @Transactional
    public void systemCancel(Long orderId, String note) {
        Order order = find(orderId);
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) return;
        OrderStatus from = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        stockService.release(stockLinesOf(order));
        orderRepository.save(order);
        recordHistory(order.getId(), from, OrderStatus.CANCELLED, note, null);
        retailerRepository.findById(order.getRetailerId()).ifPresent(r ->
                notificationService.notify(r.getUserId(), Enums.NotificationType.ORDER_UPDATE,
                        "Order " + order.getOrderNumber() + " cancelled",
                        note));
    }

    private void addDeliveredStockToRetailer(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem oi : items) {
            inventoryService.applyMovement(order.getRetailerId(), oi.getProductId(),
                    Enums.MovementType.PURCHASE, oi.getQuantity(), oi.getUnitPrice(),
                    null, null, "Order " + order.getOrderNumber() + " delivered",
                    "ORDER", order.getId());
        }
    }

    private void recordHistory(Long orderId, OrderStatus from, OrderStatus to, String note, Long changedBy) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(orderId);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setChangedBy(changedBy);
        h.setNote(note);
        historyRepository.save(h);
    }

    private void ensureParticipant(Order order) {
        var principal = CurrentUser.get();
        if (principal.roles().contains("ADMIN")) return;
        if (principal.roles().contains("RETAILER")
                && order.getRetailerId().equals(userService.currentRetailerId())) return;
        if (principal.roles().contains("SUPPLIER")
                && order.getSupplierId().equals(userService.currentSupplierId())) return;
        throw new AccessDeniedException("Not a participant of this order");
    }

    /** SECURITY: only roles permitted for a given target may drive the transition (Property 4). */
    private void authorizeTransition(Order order, OrderStatus target) {
        var principal = CurrentUser.get();
        if (principal.roles().contains("ADMIN")) return;
        Set<String> allowed = TRANSITION_ROLES.getOrDefault(target, Set.of());
        boolean roleOk = allowed.stream().anyMatch(r -> principal.roles().contains(r));
        if (!roleOk) {
            throw new AccessDeniedException("Your role cannot move an order to " + target);
        }
        // CANCELLED/RETURNED/COMPLETED by a retailer must be their own order
        if (principal.roles().contains("RETAILER")
                && !principal.roles().contains("SUPPLIER")
                && !order.getRetailerId().equals(userService.currentRetailerId())) {
            throw new AccessDeniedException("Not your order");
        }
    }

    private Order find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private List<StockService.Line> stockLinesOf(Order order) {
        return orderItemRepository.findByOrderId(order.getId()).stream()
                .map(oi -> new StockService.Line(oi.getProductId(), oi.getProductName(), oi.getQuantity()))
                .toList();
    }

    private OrderResponse toResponse(Order order) {        return OrderResponse.from(order, orderItemRepository.findByOrderId(order.getId()));
    }
}
