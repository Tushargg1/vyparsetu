package com.vyaparsetu.dev;

import com.vyaparsetu.auth.dto.AuthTokenResponse;
import com.vyaparsetu.auth.service.AuthService;
import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.inventory.repository.InventoryItemRepository;
import com.vyaparsetu.inventory.service.InventoryService;
import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.entity.OrderItem;
import com.vyaparsetu.order.entity.OrderStatus;
import com.vyaparsetu.order.repository.OrderItemRepository;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.sales.entity.CustomerSale;
import com.vyaparsetu.sales.entity.CustomerSaleItem;
import com.vyaparsetu.sales.repository.CustomerSaleItemRepository;
import com.vyaparsetu.sales.repository.CustomerSaleRepository;
import com.vyaparsetu.user.entity.Retailer;
import com.vyaparsetu.user.entity.Role;
import com.vyaparsetu.user.entity.Supplier;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.RetailerRepository;
import com.vyaparsetu.user.repository.RoleRepository;
import com.vyaparsetu.user.repository.SupplierRepository;
import com.vyaparsetu.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * DEV ONLY (non-prod). Seeds a ready-to-use test network — a distributor with a
 * catalog, a retailer already linked to it, and an admin — and issues tokens
 * without OTP so the UI test-login buttons work instantly.
 */
@Service
@Profile("!prod")
public class DevSeedService {

    private static final String TEST_INVITE_CODE = "VS-TEST0001";
    private static final String DIST_PHONE = "9000000010";
    private static final String RETAILER_PHONE = "9000000011";
    private static final String ADMIN_PHONE = "9000000012";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RetailerRepository retailerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;
    private final CustomerSaleRepository saleRepository;
    private final CustomerSaleItemRepository saleItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @PersistenceContext
    private EntityManager em;

    public DevSeedService(UserRepository userRepository, RoleRepository roleRepository,
                          RetailerRepository retailerRepository, SupplierRepository supplierRepository,
                          ProductRepository productRepository, AuthService authService,
                          InventoryItemRepository inventoryItemRepository, InventoryService inventoryService,
                          CustomerSaleRepository saleRepository, CustomerSaleItemRepository saleItemRepository,
                          OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.retailerRepository = retailerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.authService = authService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryService = inventoryService;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public AuthTokenResponse login(RoleName role) {
        Supplier distributor = ensureDistributor();
        ensureCatalog(distributor.getId());
        Retailer retailer = ensureRetailer(distributor.getId());
        ensureRetailerStock(retailer.getId(), distributor.getId());
        ensureAdmin();
        ensureHistory(retailer.getId(), distributor.getId());

        String phone = switch (role) {
            case SUPPLIER -> DIST_PHONE;
            case RETAILER -> RETAILER_PHONE;
            case ADMIN -> ADMIN_PHONE;
        };
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException("DEV_USER_MISSING", HttpStatus.INTERNAL_SERVER_ERROR, "Dev user missing"));
        return authService.issueTokensForUser(user);
    }

    private Supplier ensureDistributor() {
        User user = userRepository.findByPhone(DIST_PHONE).orElseGet(() ->
                createUser("Test Distributor", DIST_PHONE, RoleName.SUPPLIER));
        return supplierRepository.findByUserId(user.getId()).orElseGet(() -> {
            Supplier s = new Supplier();
            s.setUserId(user.getId());
            s.setBusinessName("Test Distributor");
            s.setSupplierType(Enums.SupplierType.DISTRIBUTOR);
            s.setCity("Delhi");
            s.setAddress("12, Khari Baoli, Old Delhi");
            s.setState("Delhi");
            s.setPincode("110006");
            s.setAltPhones("9000000013, 01123456789");
            s.setLocationUrl("https://maps.google.com/?q=28.6562,77.2301");
            s.setInviteCode(TEST_INVITE_CODE);
            return supplierRepository.save(s);
        });
    }

    private Retailer ensureRetailer(Long distributorId) {
        User user = userRepository.findByPhone(RETAILER_PHONE).orElseGet(() ->
                createUser("Test Retailer", RETAILER_PHONE, RoleName.RETAILER));
        Retailer r = retailerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Retailer nr = new Retailer();
            nr.setUserId(user.getId());
            nr.setShopName("Ramesh Kirana Store");
            nr.setCity("Delhi");
            nr.setAddress("Shop 4, Lajpat Nagar Market");
            nr.setState("Delhi");
            nr.setPincode("110024");
            nr.setAltPhones("9000000014");
            nr.setLocationUrl("https://maps.google.com/?q=28.5677,77.2433");
            return nr;
        });
        r.setDistributorId(distributorId);
        return retailerRepository.save(r);
    }

    /** Give the test retailer some opening stock so scan-&-sell works immediately. */
    private void ensureRetailerStock(Long retailerId, Long distributorId) {
        if (!inventoryItemRepository.findByRetailerId(retailerId).isEmpty()) {
            return;
        }
        for (Product p : productRepository.findBySupplierIdAndActiveTrue(distributorId,
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent()) {
            BigDecimal cost = p.getSellingPrice().multiply(BigDecimal.valueOf(0.8))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            inventoryService.applyMovement(retailerId, p.getId(), Enums.MovementType.PURCHASE,
                    BigDecimal.valueOf(50), cost, null, null, "Opening stock (seed)", "SEED", null);
        }
    }

    private void ensureAdmin() {
        if (userRepository.findByPhone(ADMIN_PHONE).isEmpty()) {
            createUser("Test Admin", ADMIN_PHONE, RoleName.ADMIN);
        }
    }

    private void ensureCatalog(Long supplierId) {
        if (!productRepository.findBySupplierIdAndActiveTrue(supplierId,
                org.springframework.data.domain.PageRequest.of(0, 1)).isEmpty()) {
            return;
        }
        record Seed(String name, String brand, String barcode, BigDecimal mrp, BigDecimal price, BigDecimal gst) {}
        List<Seed> seeds = List.of(
                new Seed("Maggi 2-Minute Noodles 12-Pack", "Nestle", "8901058000000", bd(180), bd(160), bd(12)),
                new Seed("Parle-G Gold 1kg", "Parle", "8901719100000", bd(120), bd(105), bd(18)),
                new Seed("Tata Salt 1kg", "Tata", "8901030700000", bd(28), bd(24), bd(5)),
                new Seed("Aashirvaad Atta 5kg", "ITC", "8901725100000", bd(260), bd(225), bd(5)),
                new Seed("Fortune Sunflower Oil 1L", "Fortune", "8906000600000", bd(160), bd(145), bd(5))
        );
        for (Seed s : seeds) {
            Product p = new Product();
            p.setSupplierId(supplierId);
            p.setName(s.name());
            p.setBrand(s.brand());
            p.setBarcode(s.barcode());
            p.setUnit("pcs");
            p.setMrp(s.mrp());
            p.setSellingPrice(s.price());
            p.setGstRate(s.gst());
            productRepository.save(p);
        }
    }

    /**
     * Seeds ~6 months of historical counter sales (bills) and distributor orders so
     * dashboards, revenue trends (day/week/month/year) and breakdowns have realistic data.
     * Runs once — guarded by existing rows for this retailer.
     */
    private void ensureHistory(Long retailerId, Long distributorId) {
        boolean hasSales = !saleRepository.findByRetailerIdOrderByCreatedAtDesc(retailerId, PageRequest.of(0, 1)).isEmpty();
        if (hasSales) {
            return; // already seeded
        }
        List<Product> products = productRepository
                .findBySupplierIdAndActiveTrue(distributorId, PageRequest.of(0, 50)).getContent();
        if (products.isEmpty()) {
            return;
        }
        Random rnd = new Random(424242L); // deterministic
        seedCounterSales(retailerId, products, rnd);
        seedDistributorOrders(retailerId, distributorId, products, rnd);
    }

    /** Counter sales (shop bills) across the last ~165 days, including today. */
    private void seedCounterSales(Long retailerId, List<Product> products, Random rnd) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int dayAgo = 165; dayAgo >= 0; dayAgo--) {
            LocalDate day = today.minusDays(dayAgo);
            // ~65% of days have sales; always include today.
            if (dayAgo != 0 && rnd.nextDouble() > 0.65) {
                continue;
            }
            int bills = 1 + rnd.nextInt(dayAgo == 0 ? 4 : 3); // a few more bills today
            for (int b = 0; b < bills; b++) {
                Instant ts = day.atTime(LocalTime.of(9 + rnd.nextInt(11), rnd.nextInt(60)))
                        .toInstant(ZoneOffset.UTC);
                createCounterSale(retailerId, products, rnd, ts);
            }
        }
    }

    private void createCounterSale(Long retailerId, List<Product> products, Random rnd, Instant ts) {
        CustomerSale sale = new CustomerSale();
        sale.setRetailerId(retailerId);
        sale = saleRepository.save(sale);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalItems = BigDecimal.ZERO;
        int lines = 1 + rnd.nextInt(3);
        for (int i = 0; i < lines; i++) {
            Product p = products.get(rnd.nextInt(products.size()));
            BigDecimal qty = BigDecimal.valueOf(1 + rnd.nextInt(6));
            BigDecimal unitPrice = p.getMrp() != null ? p.getMrp() : p.getSellingPrice();
            BigDecimal cost = p.getSellingPrice().multiply(BigDecimal.valueOf(0.8)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);

            CustomerSaleItem si = new CustomerSaleItem();
            si.setSaleId(sale.getId());
            si.setProductId(p.getId());
            si.setProductName(p.getName());
            si.setQuantity(qty);
            si.setUnitPrice(unitPrice);
            si.setCostPrice(cost);
            si.setLineTotal(lineTotal);
            saleItemRepository.save(si);

            total = total.add(lineTotal);
            totalItems = totalItems.add(qty);
        }
        sale.setTotalAmount(total);
        sale.setTotalItems(totalItems);
        saleRepository.save(sale);

        // Backdate created_at (set by @CreationTimestamp on insert) via a native update.
        em.flush();
        em.createNativeQuery("UPDATE customer_sales SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, Timestamp.from(ts))
                .setParameter(2, sale.getId())
                .executeUpdate();
    }

    /** Purchase orders placed to the distributor across the last ~5 months. */
    private void seedDistributorOrders(Long retailerId, Long distributorId, List<Product> products, Random rnd) {
        if (!orderRepository.findByRetailerId(retailerId, PageRequest.of(0, 1)).isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant now = Instant.now();
        int seq = 0;

        // Historical completed orders across the last ~5 months.
        for (int weekAgo = 22; weekAgo >= 1; weekAgo--) {
            int ordersThisWeek = rnd.nextInt(3); // 0-2 orders/week
            for (int o = 0; o < ordersThisWeek; o++) {
                LocalDate day = today.minusWeeks(weekAgo).minusDays(rnd.nextInt(7));
                if (day.isAfter(today)) day = today;
                Instant placedAt = day.atTime(LocalTime.of(10 + rnd.nextInt(8), rnd.nextInt(60)))
                        .toInstant(ZoneOffset.UTC);
                Instant delivered = placedAt.plusSeconds(2L * 86400); // delivered ~2 days later
                createDistributorOrder(retailerId, distributorId, products, rnd, placedAt, ++seq,
                        OrderStatus.COMPLETED, delivered);
            }
        }

        // Current work: pending, packed-today and delivered-today so the live views are populated.
        Instant t2 = now.minusSeconds(2L * 3600);   // 2h ago
        Instant t5 = now.minusSeconds(5L * 3600);   // 5h ago
        for (int i = 0; i < 4; i++) { // pending orders placed in the last 2 days
            Instant placedAt = now.minusSeconds((long) (rnd.nextInt(48) + 1) * 3600);
            createDistributorOrder(retailerId, distributorId, products, rnd, placedAt, ++seq,
                    OrderStatus.PENDING, null);
        }
        for (int i = 0; i < 3; i++) { // packed today (placed ~1 day ago)
            Instant placedAt = now.minusSeconds((long) (rnd.nextInt(24) + 18) * 3600);
            createDistributorOrder(retailerId, distributorId, products, rnd, placedAt, ++seq,
                    OrderStatus.PACKED, t5);
        }
        for (int i = 0; i < 3; i++) { // delivered today (placed ~2 days ago)
            Instant placedAt = now.minusSeconds((long) (rnd.nextInt(24) + 40) * 3600);
            createDistributorOrder(retailerId, distributorId, products, rnd, placedAt, ++seq,
                    OrderStatus.DELIVERED, t2);
        }
    }

    private void createDistributorOrder(Long retailerId, Long distributorId, List<Product> products,
                                        Random rnd, Instant placedAt, int seq, OrderStatus status, Instant eventAt) {
        LocalDate d = placedAt.atZone(ZoneOffset.UTC).toLocalDate();
        String orderNumber = String.format("ORD-%02d%02d%02d-%04d",
                d.getYear() % 100, d.getMonthValue(), d.getDayOfMonth(), seq);

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setRetailerId(retailerId);
        order.setSupplierId(distributorId);
        order.setOrderSource(Enums.OrderSource.CART);
        order.setPlacedAt(placedAt);
        order.setStatus(status);

        // Fulfilment timestamps based on how far the order has progressed.
        boolean accepted = status != OrderStatus.PENDING;
        boolean packed = status == OrderStatus.PACKED || status == OrderStatus.OUT_FOR_DELIVERY
                || status == OrderStatus.DELIVERED || status == OrderStatus.CASH_COLLECTED
                || status == OrderStatus.COMPLETED;
        boolean delivered = status == OrderStatus.DELIVERED || status == OrderStatus.CASH_COLLECTED
                || status == OrderStatus.COMPLETED;
        if (accepted) order.setAcceptedAt(placedAt.plusSeconds(3L * 3600));
        if (packed) order.setPackedAt(status == OrderStatus.PACKED ? eventAt : placedAt.plusSeconds(20L * 3600));
        if (delivered) order.setDeliveredAt(eventAt);

        // ~25% of orders remain unpaid (shows up as "pending to pay").
        boolean unpaid = rnd.nextDouble() < 0.25;
        order.setPaymentMode(unpaid ? Enums.PaymentMode.CREDIT : Enums.PaymentMode.UPI);
        order.setPaymentStatus(unpaid ? Enums.PaymentStatus.PENDING : Enums.PaymentStatus.PAID);
        order = orderRepository.save(order);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        int lines = 2 + rnd.nextInt(3);
        for (int i = 0; i < lines; i++) {
            Product p = products.get(rnd.nextInt(products.size()));
            BigDecimal qty = BigDecimal.valueOf(6 + rnd.nextInt(20));
            BigDecimal unitPrice = p.getSellingPrice(); // distributor price
            BigDecimal gst = p.getGstRate() != null ? p.getGstRate() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);

            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setQuantity(qty);
            oi.setUnitPrice(unitPrice);
            oi.setGstRate(gst);
            oi.setLineTotal(lineTotal);
            orderItemRepository.save(oi);

            subtotal = subtotal.add(lineTotal);
            tax = tax.add(lineTotal.multiply(gst).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        order.setSubtotal(subtotal);
        order.setTaxAmount(tax);
        BigDecimal grandTotal = subtotal.add(tax);
        order.setTotalAmount(grandTotal);
        if (order.getPaymentStatus() == Enums.PaymentStatus.PAID) {
            order.setAmountPaid(grandTotal);
            order.setLastPaymentAt(placedAt.plusSeconds(3600L * (6 + rnd.nextInt(48)))); // paid a bit after placing
        }
        orderRepository.save(order);
    }

    private User createUser(String name, String phone, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException("ROLE_MISSING", HttpStatus.INTERNAL_SERVER_ERROR, "Role missing: " + roleName));
        User u = new User();
        u.setName(name);
        u.setPhone(phone);
        u.setStatus(Enums.UserStatus.ACTIVE);
        u.setPhoneVerified(true);
        u.setRoles(Set.of(role));
        return userRepository.save(u);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }
}
