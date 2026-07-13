package com.vyaparsetu.whatsapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyaparsetu.ai.service.AiOrderService;
import com.vyaparsetu.catalog.entity.Category;
import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.CategoryRepository;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.catalog.service.StockService;
import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.notification.service.NotificationService;
import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.order.service.OrderService;
import com.vyaparsetu.user.entity.DistributorPolicy;
import com.vyaparsetu.user.entity.Retailer;
import com.vyaparsetu.user.repository.SupplierRepository;
import com.vyaparsetu.user.service.PolicyService;
import com.vyaparsetu.whatsapp.WhatsAppEnums;
import com.vyaparsetu.whatsapp.entity.ProductAlias;
import com.vyaparsetu.whatsapp.entity.RetailerRequest;
import com.vyaparsetu.whatsapp.entity.WhatsAppSession;
import com.vyaparsetu.whatsapp.repository.ProductAliasRepository;
import com.vyaparsetu.whatsapp.repository.RetailerRequestRepository;
import com.vyaparsetu.whatsapp.repository.WhatsAppSessionRepository;
import com.vyaparsetu.whatsapp.service.WhatsAppSessionData.Amb;
import com.vyaparsetu.whatsapp.service.WhatsAppSessionData.Line;
import com.vyaparsetu.whatsapp.service.WhatsAppSessionData.Opt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateful WhatsApp ordering engine. Implements the VyapaarMantra flow:
 * a message router, an interactive menu flow, a natural-language order flow
 * (AI = extraction only), a shared draft-order pipeline with DB-backed
 * validation / variant disambiguation, and an explicit confirm step.
 * AI is invoked only to extract items from free text; everything else is backend logic.
 */
@Service
public class WhatsAppConversationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppConversationService.class);
    private static final int PAGE = 9;

    // steps
    private static final String NONE = "NONE";
    private static final String MENU = "MENU";
    private static final String CAT = "CAT";
    private static final String PROD = "PROD";
    private static final String QTY = "QTY";
    private static final String DISAMBIG = "DISAMBIG";
    private static final String REVIEW = "REVIEW";
    private static final String EDIT = "EDIT";
    private static final String PRICE = "PRICE";
    // registration steps
    private static final String REG_MENU = "REG_MENU";
    private static final String REG_SHOP = "REG_SHOP";
    private static final String REG_OWNER = "REG_OWNER";
    private static final String REG_LINK = "REG_LINK";
    private static final String REG_PENDING = "REG_PENDING";

    private final WhatsAppSessionRepository sessionRepo;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AiOrderService aiOrderService;
    private final ProductAliasRepository aliasRepo;
    private final RetailerRequestRepository requestRepo;
    private final SupplierRepository supplierRepo;
    private final NotificationService notificationService;
    private final AppProperties props;
    private final StockService stockService;
    private final PolicyService policyService;
    private final com.vyaparsetu.analytics.service.AnalyticsService analyticsService;
    private final ObjectMapper mapper = new ObjectMapper();

    public WhatsAppConversationService(WhatsAppSessionRepository sessionRepo, ProductRepository productRepository,
                                       CategoryRepository categoryRepository, OrderRepository orderRepository,
                                       OrderService orderService, AiOrderService aiOrderService,
                                       ProductAliasRepository aliasRepo, RetailerRequestRepository requestRepo,
                                       SupplierRepository supplierRepo, NotificationService notificationService,
                                       AppProperties props, StockService stockService, PolicyService policyService,
                                       com.vyaparsetu.analytics.service.AnalyticsService analyticsService) {
        this.sessionRepo = sessionRepo;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.aiOrderService = aiOrderService;
        this.aliasRepo = aliasRepo;
        this.requestRepo = requestRepo;
        this.supplierRepo = supplierRepo;
        this.notificationService = notificationService;
        this.props = props;
        this.stockService = stockService;
        this.policyService = policyService;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public String handle(Long supplierId, Retailer retailer, String phone, String text) {
        WhatsAppSession session = sessionRepo.findBySupplierIdAndPhone(supplierId, phone)
                .orElseGet(() -> {
                    WhatsAppSession s = new WhatsAppSession();
                    s.setSupplierId(supplierId);
                    s.setPhone(phone);
                    s.setStep(NONE);
                    return s;
                });
        WhatsAppSessionData data = read(session);
        String t = text.trim();
        String low = t.toLowerCase();

        // Draft expiry: abandon stale carts after the configured TTL.
        if (isExpired(session)) {
            data = new WhatsAppSessionData();
            session.setStep(NONE);
        }

        // Remember who we're talking to (for analytics attribution).
        if (retailer != null) data.setRetailerId(retailer.getId());

        // Universal commands.
        if (low.equals("cancel") || low.equals("exit") || low.equals("stop")) {
            return finish(session, data, "Cancelled. Send an order anytime, or type *menu*.");
        }
        if (low.equals("menu") || low.equals("start")) {
            return mainMenu(session, data);
        }

        // Continue an active step.
        return switch (session.getStep()) {
            case MENU -> onMenuPick(session, data, retailer, t);
            case CAT -> onCategoryPick(session, data, supplierId, t);
            case PROD -> onProductPick(session, data, t);
            case QTY -> onQtyEntered(session, data, t);
            case DISAMBIG -> onDisambigPick(session, data, t);
            case REVIEW -> onReview(session, data, supplierId, retailer, t);
            case EDIT -> onEditPick(session, data, t);
            case PRICE -> onPriceName(session, data, supplierId, t);
            default -> classify(session, data, supplierId, retailer, t, low);
        };
    }

    // ---------------- registration (unknown numbers) ----------------

    /**
     * Onboarding conversation for a phone not yet linked to any retailer.
     * Offers "create new account" or "link existing", collects details, stores a
     * pending request and notifies the distributor. Replies "pending" until approved.
     */
    @Transactional
    public String handleRegistration(Long supplierId, String phone, String text) {
        WhatsAppSession session = sessionRepo.findBySupplierIdAndPhone(supplierId, phone)
                .orElseGet(() -> {
                    WhatsAppSession s = new WhatsAppSession();
                    s.setSupplierId(supplierId);
                    s.setPhone(phone);
                    s.setStep(NONE);
                    return s;
                });
        WhatsAppSessionData data = read(session);
        String t = text.trim();
        String low = t.toLowerCase();

        // Already requested and awaiting approval.
        boolean alreadyPending = requestRepo
                .findBySupplierIdAndPhoneAndStatus(supplierId, phone, WhatsAppEnums.RequestStatus.PENDING)
                .isPresent();

        return switch (session.getStep()) {
            case REG_MENU -> onRegChoice(session, data, t);
            case REG_SHOP -> {
                data.setRegShopName(t);
                yield save(session, data, REG_OWNER, "And the owner's name?");
            }
            case REG_OWNER -> {
                data.setRegOwner(t);
                yield submitRequest(session, data, supplierId, phone, "NEW");
            }
            case REG_LINK -> {
                data.setRegShopName("Link to " + t);
                yield submitRequest(session, data, supplierId, phone, "LINK");
            }
            case REG_PENDING -> "⏳ Your account is pending approval from the distributor. "
                    + "We'll message you as soon as it's active.";
            default -> {
                if (alreadyPending) {
                    save(session, data, REG_PENDING, null);
                    yield "⏳ Your account is pending approval from the distributor.";
                }
                yield regMenu(session, data);
            }
        };
    }

    private String regMenu(WhatsAppSession s, WhatsAppSessionData d) {
        d.getOptions().clear();
        d.getOptions().add(opt("🆕 Create a new retailer account", "REG_NEW"));
        d.getOptions().add(opt("🔗 Link this number to an existing account", "REG_LINK"));
        return save(s, d, REG_MENU,
                "Welcome to VyapaarMantra! 👋 This number isn't registered yet.\n"
                        + render("How would you like to continue?", d.getOptions()));
    }

    private String onRegChoice(WhatsAppSession s, WhatsAppSessionData d, String t) {
        Opt o = pick(d, t);
        if (o == null) return reprompt(s, d, "Reply 1 to create a new account or 2 to link an existing one.");
        if ("REG_NEW".equals(o.getCode())) {
            d.setRegType("NEW");
            return save(s, d, REG_SHOP, "Great! What's your *shop name*?");
        }
        d.setRegType("LINK");
        return save(s, d, REG_LINK, "Please send the *phone number* already registered with this distributor.");
    }

    private String submitRequest(WhatsAppSession s, WhatsAppSessionData d, Long supplierId, String phone, String type) {
        RetailerRequest req = requestRepo
                .findBySupplierIdAndPhoneAndStatus(supplierId, phone, WhatsAppEnums.RequestStatus.PENDING)
                .orElseGet(RetailerRequest::new);
        req.setSupplierId(supplierId);
        req.setPhone(phone);
        req.setStatus(WhatsAppEnums.RequestStatus.PENDING);
        req.setShopName(d.getRegShopName());
        req.setOwnerName(d.getRegOwner());
        req.setMessage("LINK".equals(type) ? "Requested to link an existing account" : "New account via WhatsApp");
        requestRepo.save(req);

        // Notify the distributor inside the app.
        supplierRepo.findById(supplierId).ifPresent(sup -> notificationService.notify(
                sup.getUserId(), Enums.NotificationType.SYSTEM, "New retailer request",
                (d.getRegShopName() != null ? d.getRegShopName() : "A customer") + " (" + phone + ") wants to order on WhatsApp."));

        save(s, d, REG_PENDING, null);
        return "✅ Thanks! Your details have been sent to the distributor for approval. "
                + "You'll be able to order as soon as they confirm your account.";
    }

    /** Clear any session for a phone (e.g. once a retailer is approved). */
    @Transactional
    public void clearSession(Long supplierId, String phone) {
        sessionRepo.findBySupplierIdAndPhone(supplierId, phone).ifPresent(sessionRepo::delete);
    }

    private boolean isExpired(WhatsAppSession s) {
        if (s.getId() == null || s.getUpdatedAt() == null || NONE.equals(s.getStep())) return false;
        long ttl = Math.max(1, props.getFeatures().getWhatsapp().getDraftTtlHours());
        return s.getUpdatedAt().isBefore(Instant.now().minus(ttl, ChronoUnit.HOURS));
    }

    // ---------------- router ----------------

    private String classify(WhatsAppSession s, WhatsAppSessionData d, Long supplierId, Retailer retailer,
                            String t, String low) {
        // A quantity + product word is a strong order signal — handle before keyword intents
        // (avoids product names like "Stock..." colliding with the "stock" intent).
        boolean strongOrder = low.matches(".*\\d.*") && low.matches(".*[a-z\\u0900-\\u097f]{2,}.*");
        if (strongOrder) {
            return startNlOrder(s, d, supplierId, t);
        }
        if (isGreeting(low)) return mainMenu(s, d);
        if (hasAny(low, "price", "rate", "mrp", "daam", "dam", "bhav", "kitne", "kitna", "how much", "cost")) {
            String q = stripWords(t);
            return q.isBlank() ? ask(s, d, PRICE, "Which product's price would you like?") : priceReply(supplierId, q, s, d);
        }
        if (hasAny(low, "status", "track", "kahan", "kaha", "delivery", "order kab")) {
            return finish(s, d, orderStatus(retailer));
        }
        if (hasAny(low, "pending", "bill", "payment", "due", "udhaar", "baki", "balance")) {
            return finish(s, d, paymentsReply(retailer));
        }
        if (hasAny(low, "stock", "available", "availability", "milega", "milegi", "hai kya")) {
            String q = stripWords(t);
            return q.isBlank() ? ask(s, d, PRICE, "Which product are you looking for?") : priceReply(supplierId, q, s, d);
        }
        if (looksLikeOrder(low)) {
            return startNlOrder(s, d, supplierId, t);
        }
        return mainMenu(s, d);
    }

    private boolean looksLikeOrder(String low) {
        if (low.matches(".*\\d.*") && low.matches(".*[a-z\\u0900-\\u097f]{2,}.*")) return true;
        return hasAny(low, "same as last", "wahi", "repeat", "dobara", "add ", "reduce", "badha", "ghata",
                "bana do", "chahiye", "chahie", "bhej", "order karo", "mangwa", "mangao", "want", "need",
                "de do", "dedo", "le lo", "bhejo", "send me");
    }

    private boolean isGreeting(String low) {
        return low.matches("^(hi+|hey+|hello+|helo|hii+|namaste|namaskar|ram ram|good\\s*(morning|afternoon|evening)|gm|gn|hlo)\\b.*")
                && low.length() <= 25;
    }

    // ---------------- main menu ----------------

    private String mainMenu(WhatsAppSession s, WhatsAppSessionData d) {
        d.getOptions().clear();
        d.getOptions().add(opt("🛒 Place Order", "PLACE"));
        d.getOptions().add(opt("💰 Price Enquiry", "PRICE"));
        d.getOptions().add(opt("📦 Order Status", "STATUS"));
        d.getOptions().add(opt("📄 Bill Status", "BILL"));
        d.getOptions().add(opt("💳 Pending Payments", "PAY"));
        d.getOptions().add(opt("☎ Contact Sales", "CONTACT"));
        return save(s, d, MENU, render("Welcome 👋 Please choose an option:", d.getOptions()));
    }

    private String onMenuPick(WhatsAppSession s, WhatsAppSessionData d, Retailer retailer, String t) {
        Opt o = pick(d, t);
        if (o == null) return reprompt(s, d, "Please reply with a number from the menu.");
        return switch (o.getCode()) {
            case "PLACE" -> showCategories(s, d, retailer.getDistributorId());
            case "PRICE" -> ask(s, d, PRICE, "Type the product name to see its price.");
            case "STATUS" -> finish(s, d, orderStatus(retailer));
            case "BILL" -> finish(s, d, "Bill status: your last invoice is sent after each delivered order. Reply *menu* for more.");
            case "PAY" -> finish(s, d, paymentsReply(retailer));
            case "CONTACT" -> finish(s, d, "Our sales team will reach out shortly. You can also reply with your order anytime.");
            default -> reprompt(s, d, "Please reply with a valid option number.");
        };
    }

    // ---------------- place order (menu / browse) ----------------

    private String showCategories(WhatsAppSession s, WhatsAppSessionData d, Long supplierId) {
        List<Product> prods = productRepository
                .findBySupplierIdAndActiveTrue(supplierId, PageRequest.of(0, 200)).getContent();
        if (prods.isEmpty()) {
            return finish(s, d, "No products are available right now. Please try later.");
        }
        // group by category
        Map<Long, String> catNames = new LinkedHashMap<>();
        for (Product p : prods) {
            Long cid = p.getCategoryId() == null ? -1L : p.getCategoryId();
            catNames.putIfAbsent(cid, null);
        }
        categoryRepository.findAllById(catNames.keySet().stream().filter(id -> id >= 0).toList())
                .forEach(c -> catNames.put(c.getId(), c.getName()));

        if (catNames.size() <= 1) {
            // single/no category → list products directly
            return listProducts(s, d, supplierId, catNames.keySet().iterator().next());
        }
        d.getOptions().clear();
        for (Map.Entry<Long, String> e : catNames.entrySet()) {
            String label = e.getValue() != null ? e.getValue() : "Other";
            d.getOptions().add(opt(label, e.getKey(), "CAT"));
        }
        return save(s, d, CAT, render("Choose a category:", d.getOptions()));
    }

    private String onCategoryPick(WhatsAppSession s, WhatsAppSessionData d, Long supplierId, String t) {
        Opt o = pick(d, t);
        if (o == null) return reprompt(s, d, "Reply with a category number.");
        return listProducts(s, d, supplierId, o.getRef());
    }

    private String listProducts(WhatsAppSession s, WhatsAppSessionData d, Long supplierId, Long categoryId) {
        List<Product> prods = productRepository
                .findBySupplierIdAndActiveTrue(supplierId, PageRequest.of(0, 200)).getContent();
        d.getOptions().clear();
        for (Product p : prods) {
            Long cid = p.getCategoryId() == null ? -1L : p.getCategoryId();
            if (categoryId != null && !categoryId.equals(cid)) continue;
            if (d.getOptions().size() >= PAGE) break;
            d.getOptions().add(opt(p.getName() + " — ₹" + p.getSellingPrice(), p.getId(), "PROD"));
        }
        if (d.getOptions().isEmpty()) {
            return finish(s, d, "No products in that category.");
        }
        return save(s, d, PROD, render("Select a product:", d.getOptions()));
    }

    private String onProductPick(WhatsAppSession s, WhatsAppSessionData d, String t) {
        Opt o = pick(d, t);
        if (o == null) return reprompt(s, d, "Reply with a product number.");
        d.setPendingProductId(o.getRef());
        Product p = productRepository.findById(o.getRef()).orElse(null);
        d.setPendingProductName(p != null ? p.getName() : o.getLabel());
        return save(s, d, QTY, "How many *" + d.getPendingProductName() + "*? Reply with a number.");
    }

    private String onQtyEntered(WhatsAppSession s, WhatsAppSessionData d, String t) {
        BigDecimal qty = number(t);
        if (qty == null || qty.signum() <= 0) {
            return reprompt(s, d, "Please reply with a valid quantity (e.g. 5).");
        }
        Product p = d.getPendingProductId() == null ? null : productRepository.findById(d.getPendingProductId()).orElse(null);
        if (p == null) {
            return mainMenu(s, d);
        }
        addToDraft(d, p, qty.doubleValue());
        d.setPendingProductId(null);
        d.setPendingProductName(null);
        return advance(s, d, "");
    }

    // ---------------- natural language order ----------------

    private String startNlOrder(WhatsAppSession s, WhatsAppSessionData d, Long supplierId, String text) {
        List<AiOrderService.ExtractedItem> items = aiOrderService.extractItems(text);
        if (items.isEmpty()) {
            // AI could not extract a single item: track it and alert the distributor.
            try {
                analyticsService.recordAiExtractionFailed(supplierId, d.getRetailerId(), text);
            } catch (RuntimeException ignored) {
            }
            notifyDistributor(supplierId, com.vyaparsetu.common.enums.Enums.NotificationType.AI_EXTRACTION_FAILED,
                    "Could not understand a WhatsApp order",
                    "Message: \"" + (text == null ? "" : text) + "\"");
            return mainMenu(s, d);
        }
        List<String> unresolved = new ArrayList<>();
        for (AiOrderService.ExtractedItem item : items) {
            List<Product> candidates = resolveCandidates(supplierId, item.name());
            if (candidates.isEmpty()) {
                unresolved.add(item.name());
                try {
                    analyticsService.recordValidationFailure(supplierId, d.getRetailerId(),
                            "UNRESOLVED_PRODUCT:" + item.name());
                } catch (RuntimeException ignored) {
                }
            } else if (candidates.size() == 1) {
                addToDraft(d, candidates.get(0), item.qty().doubleValue());
            } else {
                d.getAmbiguous().add(new Amb(item.name(), item.qty().doubleValue(),
                        candidates.stream().map(Product::getId).toList()));
            }
        }
        String note = unresolved.isEmpty() ? "" : "⚠ Not found: " + String.join(", ", unresolved);
        return advance(s, d, note);
    }

    /** Best-effort supplier notification helper. */
    private void notifyDistributor(Long supplierId, com.vyaparsetu.common.enums.Enums.NotificationType type,
                                   String title, String body) {
        try {
            supplierRepo.findById(supplierId).ifPresent(sup ->
                    notificationService.notify(sup.getUserId(), type, title, body));
        } catch (RuntimeException ignored) {
        }
    }

    /** Resolve aliases first (no LLM), then fall back to catalog search. */
    private List<Product> resolveCandidates(Long supplierId, String term) {
        String q = term == null ? "" : term.trim();
        if (q.isEmpty()) return List.of();
        ProductAlias a = aliasRepo.findBySupplierIdAndAliasIgnoreCase(supplierId, q).orElse(null);
        if (a != null) {
            try {
                analyticsService.recordAliasUsed(supplierId, null, q);
            } catch (RuntimeException ignored) {
            }
            if (a.getProductId() != null) {
                Product p = productRepository.findById(a.getProductId()).orElse(null);
                if (p != null && p.isActive()) return List.of(p);
            }
            if (a.getCanonical() != null && !a.getCanonical().isBlank()) {
                q = a.getCanonical();
            }
        }
        return productRepository.search(q, null, supplierId, PageRequest.of(0, 6)).getContent();
    }

    /** Move the order forward: disambiguate → ask missing quantity → review. */
    private String advance(WhatsAppSession s, WhatsAppSessionData d, String note) {
        if (!d.getAmbiguous().isEmpty()) {
            return presentDisambig(s, d, note);
        }
        Line missing = firstMissingQty(d);
        if (missing != null) {
            d.setPendingProductId(missing.getProductId());
            d.setPendingProductName(missing.getName());
            return save(s, d, QTY, (note.isBlank() ? "" : note + "\n")
                    + "How many *" + missing.getName() + "*? Reply with a number.");
        }
        if (d.getDraft().isEmpty()) {
            return finish(s, d, "I couldn't match any products." + (note.isBlank() ? "" : "\n" + note)
                    + "\nType *menu* to browse the catalog.");
        }
        return review(s, d, note);
    }

    private Line firstMissingQty(WhatsAppSessionData d) {
        for (Line l : d.getDraft()) {
            if (l.getQty() <= 0) return l;
        }
        return null;
    }

    private String presentDisambig(WhatsAppSession s, WhatsAppSessionData d, String note) {
        Amb a = d.getAmbiguous().get(0);
        List<Product> candidates = productRepository.findAllById(a.getCandidates());
        d.getOptions().clear();
        for (Product p : candidates) {
            d.getOptions().add(opt(p.getName() + " — ₹" + p.getSellingPrice(), p.getId(), "VAR"));
        }
        String header = "Which *" + a.getTerm() + "* did you mean? (qty " + trimNum(a.getQty()) + ")";
        return save(s, d, DISAMBIG, (note.isBlank() ? "" : note + "\n\n") + render(header, d.getOptions()));
    }

    private String onDisambigPick(WhatsAppSession s, WhatsAppSessionData d, String t) {
        Opt o = pick(d, t);
        if (o == null) return reprompt(s, d, "Reply with a number to pick the right variant.");
        Amb a = d.getAmbiguous().remove(0);
        Product p = productRepository.findById(o.getRef()).orElse(null);
        if (p != null) addToDraft(d, p, a.getQty());
        return advance(s, d, "");
    }

    // ---------------- review / confirm / edit ----------------

    private String review(WhatsAppSession s, WhatsAppSessionData d) {
        return review(s, d, "");
    }

    private String review(WhatsAppSession s, WhatsAppSessionData d, String note) {
        if (d.getDraft().isEmpty()) {
            return finish(s, d, "Your order is empty. Type *menu* to start again.");
        }
        StringBuilder sb = new StringBuilder();
        if (!note.isBlank()) sb.append(note).append("\n");
        sb.append("🧾 *Order Summary*\n");
        double subtotal = 0, gst = 0;
        for (Line l : d.getDraft()) {
            double lt = l.getPrice() * l.getQty();
            subtotal += lt;
            gst += lt * l.getGstRate() / 100.0;
            sb.append("• ").append(l.getName()).append(" × ").append(trimNum(l.getQty()))
                    .append(" = ₹").append(money(lt)).append("\n");
        }
        sb.append("Subtotal: ₹").append(money(subtotal)).append("\n");
        sb.append("GST: ₹").append(money(gst)).append("\n");
        sb.append("*Total: ₹").append(money(subtotal + gst)).append("*\n");
        d.getOptions().clear();
        d.getOptions().add(opt("✅ Confirm Order", "CONFIRM"));
        d.getOptions().add(opt("➕ Add more items", "ADD"));
        d.getOptions().add(opt("✏ Edit (remove item)", "EDIT"));
        d.getOptions().add(opt("❌ Cancel", "CANCEL"));
        return save(s, d, REVIEW, sb + render("", d.getOptions()));
    }

    private String onReview(WhatsAppSession s, WhatsAppSessionData d, Long supplierId, Retailer retailer, String t) {
        String low = t.toLowerCase();
        // "add 2 Coke" / "5 more Maggi" while reviewing → append to the existing draft.
        if (looksLikeOrder(low) && low.matches(".*\\d.*")) {
            return startNlOrder(s, d, supplierId, t);
        }
        Opt o = pick(d, t);
        String code = o != null ? o.getCode() : (low.startsWith("y") || low.contains("confirm") ? "CONFIRM"
                : low.startsWith("n") || low.contains("cancel") ? "CANCEL" : null);
        if (code == null) return reprompt(s, d, "Reply 1 to confirm, 2 to add more, 3 to edit, 4 to cancel.");
        return switch (code) {
            case "CONFIRM" -> createOrder(s, d, supplierId, retailer);
            case "ADD" -> showCategories(s, d, supplierId);
            case "EDIT" -> startEdit(s, d);
            case "CANCEL" -> finish(s, d, "Order cancelled. Send a new list anytime.");
            default -> reprompt(s, d, "Please choose a valid option.");
        };
    }

    private String startEdit(WhatsAppSession s, WhatsAppSessionData d) {
        d.getOptions().clear();
        for (Line l : d.getDraft()) {
            d.getOptions().add(opt(l.getName() + " × " + trimNum(l.getQty()), l.getProductId(), "DEL"));
        }
        return save(s, d, EDIT, render("Reply a number to remove that item:", d.getOptions()));
    }

    private String onEditPick(WhatsAppSession s, WhatsAppSessionData d, String t) {
        Opt o = pick(d, t);
        if (o == null) return reprompt(s, d, "Reply with the item number to remove.");
        d.getDraft().removeIf(l -> l.getProductId().equals(o.getRef()));
        return review(s, d);
    }

    private String createOrder(WhatsAppSession s, WhatsAppSessionData d, Long supplierId, Retailer retailer) {
        if (d.getDraft().isEmpty()) {
            return finish(s, d, "Your order is empty. Type *menu* to start again.");
        }
        DistributorPolicy policy = policyService.policyFor(supplierId);

        // Minimum order quantity / value (backend policy — never the LLM).
        double totalQty = d.getDraft().stream().mapToDouble(Line::getQty).sum();
        double orderValue = d.getDraft().stream().mapToDouble(l -> l.getPrice() * l.getQty()).sum();
        if (policy.getMinOrderQty().signum() > 0 && totalQty < policy.getMinOrderQty().doubleValue()) {
            return reprompt(s, d, "Minimum order is " + trimNum(policy.getMinOrderQty().doubleValue())
                    + " units. Add more items (reply 2) or cancel (4).");
        }
        if (policy.getMinOrderValue().signum() > 0 && orderValue < policy.getMinOrderValue().doubleValue()) {
            return reprompt(s, d, "Minimum order value is ₹" + money(policy.getMinOrderValue().doubleValue())
                    + ". Add more items (reply 2) or cancel (4).");
        }

        // Stock validation against the distributor's out-of-stock policy.
        List<StockService.Line> requested = d.getDraft().stream()
                .map(l -> new StockService.Line(l.getProductId(), l.getName(), BigDecimal.valueOf(l.getQty())))
                .toList();
        StockService.Result check = stockService.validate(requested, policy.getOutOfStockBehavior());
        if (!check.ok()) {
            return reprompt(s, d, "⚠ Some items are short on stock:\n• " + String.join("\n• ", check.messages())
                    + "\nReduce the quantity (reply 3 to edit) or cancel (4).");
        }
        if (check.lines().isEmpty()) {
            return finish(s, d, "Sorry, those items are out of stock right now. Please try later.");
        }

        List<OrderService.LineItem> items = check.lines().stream()
                .map(l -> new OrderService.LineItem(l.productId(), l.qty()))
                .toList();
        Order order = orderService.createOrderForRetailer(retailer.getId(), supplierId, items,
                Enums.OrderSource.AI, Enums.PaymentMode.COD, null, null);

        String note = check.messages().isEmpty() ? "" : "\n(" + String.join(" ", check.messages()) + ")";
        return finish(s, d, "✅ Order *" + order.getOrderNumber() + "* placed! Total ₹"
                + money(order.getTotalAmount().doubleValue()) + "." + note
                + "\nStatus: PENDING — your distributor will accept it shortly.");
    }

    // ---------------- info intents (DB only) ----------------

    private String priceReply(Long supplierId, String query, WhatsAppSession s, WhatsAppSessionData d) {
        Product p = findProduct(supplierId, query);
        if (p == null) return finish(s, d, "Couldn't find \"" + query + "\". Type *menu* to browse the catalog.");
        return finish(s, d, p.getName() + " — ₹" + p.getSellingPrice() + " (MRP ₹" + p.getMrp() + ")."
                + "\nReply with a quantity to order, e.g. \"5 " + p.getName() + "\".");
    }

    private String onPriceName(WhatsAppSession s, WhatsAppSessionData d, Long supplierId, String t) {
        return priceReply(supplierId, stripWords(t), s, d);
    }

    private String orderStatus(Retailer retailer) {
        Order last = orderRepository.findTopByRetailerIdOrderByIdDesc(retailer.getId()).orElse(null);
        if (last == null) return "You have no orders yet. Send a list like \"2 Maggi, 5 Parle-G\" to place one.";
        return "Your latest order " + last.getOrderNumber() + " is *" + last.getStatus() + "* (₹"
                + money(last.getTotalAmount().doubleValue()) + ").";
    }

    private String paymentsReply(Retailer retailer) {
        List<Order> orders = orderRepository
                .findByRetailerId(retailer.getId(), PageRequest.of(0, 200)).getContent();
        double due = 0;
        int count = 0;
        for (Order o : orders) {
            boolean open = o.getPaymentStatus() == Enums.PaymentStatus.PENDING
                    || o.getPaymentStatus() == Enums.PaymentStatus.PARTIAL;
            boolean live = o.getStatus() != com.vyaparsetu.order.entity.OrderStatus.REJECTED
                    && o.getStatus() != com.vyaparsetu.order.entity.OrderStatus.CANCELLED;
            if (open && live) {
                due += o.getTotalAmount() == null ? 0 : o.getTotalAmount().doubleValue();
                count++;
            }
        }
        if (count == 0) return "You have no pending payments. 🎉";
        return "Pending payments: ₹" + money(due) + " across " + count + " order(s).";
    }

    private Product findProduct(Long supplierId, String query) {
        if (query == null || query.isBlank()) return null;
        List<Product> r = productRepository.search(query, null, supplierId, PageRequest.of(0, 1)).getContent();
        return r.isEmpty() ? null : r.get(0);
    }

    // ---------------- helpers ----------------

    private void addToDraft(WhatsAppSessionData d, Product p, double qty) {
        for (Line l : d.getDraft()) {
            if (l.getProductId().equals(p.getId())) {
                l.setQty(l.getQty() + qty);
                return;
            }
        }
        d.getDraft().add(new Line(p.getId(), p.getName(), qty,
                p.getSellingPrice().doubleValue(), p.getGstRate().doubleValue()));
    }

    private Opt pick(WhatsAppSessionData d, String text) {
        String t = text.trim();
        if (t.matches("\\d+")) {
            int n = Integer.parseInt(t);
            if (n >= 1 && n <= d.getOptions().size()) return d.getOptions().get(n - 1);
        }
        String low = t.toLowerCase();
        for (Opt o : d.getOptions()) {
            if (o.getLabel() != null && o.getLabel().toLowerCase().contains(low) && low.length() >= 3) return o;
        }
        return null;
    }

    private BigDecimal number(String t) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(t);
        return m.find() ? new BigDecimal(m.group(1)) : null;
    }

    private boolean hasAny(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    private String stripWords(String text) {
        return text.replaceAll("(?i)\\b(price|rate|mrp|daam|dam|bhav|kitne|kitna|how much|cost|stock|available|availability|milega|milegi|hai|kya|ka|ki|ke|of|the|please|pls|whats|what is|is|me|mujhe|chahiye)\\b", " ")
                .replaceAll("[?.!,]", " ").replaceAll("\\s+", " ").trim();
    }

    private Opt opt(String label, String code) {
        return new Opt(label, null, code);
    }

    private Opt opt(String label, Long ref, String code) {
        return new Opt(label, ref, code);
    }

    private String render(String header, List<Opt> opts) {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isBlank()) sb.append(header).append("\n");
        for (int i = 0; i < opts.size(); i++) {
            sb.append(i + 1).append(". ").append(opts.get(i).getLabel()).append("\n");
        }
        sb.append("_Reply with a number._");
        return sb.toString();
    }

    private String trimNum(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private String money(double v) {
        return String.format("%.2f", v);
    }

    // ---------------- session persistence ----------------

    private String ask(WhatsAppSession s, WhatsAppSessionData d, String step, String message) {
        return save(s, d, step, message);
    }

    private String reprompt(WhatsAppSession s, WhatsAppSessionData d, String message) {
        // keep current step + options, just re-ask
        save(s, d, s.getStep(), null);
        return message;
    }

    private String finish(WhatsAppSession s, WhatsAppSessionData d, String message) {
        d.getOptions().clear();
        d.getDraft().clear();
        d.getAmbiguous().clear();
        d.setPendingProductId(null);
        d.setPendingProductName(null);
        save(s, d, NONE, null);
        return message;
    }

    private String save(WhatsAppSession s, WhatsAppSessionData d, String step, String reply) {
        try {
            s.setStep(step);
            s.setDataJson(mapper.writeValueAsString(d));
            sessionRepo.save(s);
        } catch (Exception e) {
            log.error("[WA] session save failed: {}", e.getMessage());
        }
        return reply;
    }

    private WhatsAppSessionData read(WhatsAppSession s) {
        if (s.getDataJson() == null || s.getDataJson().isBlank()) return new WhatsAppSessionData();
        try {
            return mapper.readValue(s.getDataJson(), WhatsAppSessionData.class);
        } catch (Exception e) {
            return new WhatsAppSessionData();
        }
    }
}
