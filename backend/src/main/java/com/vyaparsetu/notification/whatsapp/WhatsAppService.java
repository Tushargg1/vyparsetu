package com.vyaparsetu.notification.whatsapp;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.user.entity.Retailer;
import com.vyaparsetu.user.entity.Role;
import com.vyaparsetu.user.entity.Supplier;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.RetailerRepository;
import com.vyaparsetu.user.repository.RoleRepository;
import com.vyaparsetu.user.repository.SupplierRepository;
import com.vyaparsetu.user.repository.UserRepository;
import com.vyaparsetu.whatsapp.WhatsAppEnums;
import com.vyaparsetu.whatsapp.entity.RetailerRequest;
import com.vyaparsetu.whatsapp.entity.WhatsAppSettings;
import com.vyaparsetu.whatsapp.repository.RetailerPhoneRepository;
import com.vyaparsetu.whatsapp.repository.RetailerRequestRepository;
import com.vyaparsetu.whatsapp.repository.WhatsAppSettingsRepository;
import com.vyaparsetu.whatsapp.service.ConversationLogService;
import com.vyaparsetu.whatsapp.service.WhatsAppConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Inbound WhatsApp gateway: applies AI/takeover/business-hours gates, handles
 * JOIN and first-time customer onboarding, then hands linked retailers to the
 * stateful {@link WhatsAppConversationService} (router → menu / NL order →
 * draft → validation → confirm). {@code toPhone} routes to the right distributor.
 */
@Service
public class WhatsAppService {

    private final UserRepository userRepository;
    private final RetailerRepository retailerRepository;
    private final SupplierRepository supplierRepository;
    private final RoleRepository roleRepository;
    private final WhatsAppClient whatsAppClient;
    private final WhatsAppSettingsRepository settingsRepo;
    private final RetailerRequestRepository requestRepo;
    private final RetailerPhoneRepository phoneRepo;
    private final WhatsAppConversationService conversationService;
    private final ConversationLogService conversationLog;

    public WhatsAppService(UserRepository userRepository, RetailerRepository retailerRepository,
                           SupplierRepository supplierRepository, RoleRepository roleRepository,
                           WhatsAppClient whatsAppClient, WhatsAppSettingsRepository settingsRepo,
                           RetailerRequestRepository requestRepo, RetailerPhoneRepository phoneRepo,
                           WhatsAppConversationService conversationService, ConversationLogService conversationLog) {
        this.userRepository = userRepository;
        this.retailerRepository = retailerRepository;
        this.supplierRepository = supplierRepository;
        this.roleRepository = roleRepository;
        this.whatsAppClient = whatsAppClient;
        this.settingsRepo = settingsRepo;
        this.requestRepo = requestRepo;
        this.phoneRepo = phoneRepo;
        this.conversationService = conversationService;
        this.conversationLog = conversationLog;
    }

    /** Backwards-compatible entry point without business-number routing. */
    @Transactional
    public String handleInbound(String fromPhone, String text) {
        return handleInbound(fromPhone, null, text, null);
    }

    @Transactional
    public String handleInbound(String fromPhone, String toPhone, String text) {
        return handleInbound(fromPhone, toPhone, text, null);
    }

    @Transactional
    public String handleInbound(String fromPhone, String toPhone, String text, String providerMessageId) {
        if (fromPhone == null || text == null || text.isBlank()) {
            return reply(fromPhone, "Sorry, I couldn't read your message.");
        }
        // Idempotency: ignore duplicate webhook deliveries.
        if (providerMessageId != null && conversationLog.isDuplicate(providerMessageId)) {
            return "";
        }
        WhatsAppSettings settings = resolveSettings(toPhone);
        Long supplierContextId = settings != null ? settings.getSupplierId() : null;
        String phone = normalizePhone(fromPhone);

        String reply = process(phone, text.trim(), settings, supplierContextId);

        if (supplierContextId != null) {
            conversationLog.logInbound(supplierContextId, phone, text, providerMessageId);
            conversationLog.logOutbound(supplierContextId, phone, reply);
        }
        return reply;
    }

    /**
     * Drive the assistant as if a message arrived for the given distributor.
     * Used by the in-app test console so the full flow works without a live provider.
     */
    @Transactional
    public String simulateInbound(Long supplierId, String fromPhone, String text) {
        if (fromPhone == null || text == null || text.isBlank()) {
            return "Please type a message.";
        }
        WhatsAppSettings settings = settingsRepo.findBySupplierId(supplierId).orElse(null);
        String phone = normalizePhone(fromPhone);
        String reply = process(phone, text.trim(), settings, supplierId);
        conversationLog.logInbound(supplierId, phone, text, null);
        conversationLog.logOutbound(supplierId, phone, reply);
        return reply;
    }

    private String process(String phone, String trimmed, WhatsAppSettings settings, Long supplierContextId) {
        // Human takeover pauses the AI entirely.
        if (settings != null && settings.isHumanTakeover()) {
            return reply(phone, "A team member will reply to you shortly. Thank you for your patience.");
        }
        // AI / auto-reply switched off: stay silent.
        if (settings != null && (!settings.isAiEnabled() || !settings.isAutoReply())) {
            return "";
        }
        // Outside business hours: send a polite after-hours notice and stop.
        if (settings != null && !withinBusinessHours(settings)) {
            return reply(phone, "Thanks for your message! Our ordering hours are "
                    + settings.getBusinessHoursStart() + "–" + settings.getBusinessHoursEnd()
                    + ". We'll get back to you then.");
        }

        if (trimmed.toUpperCase().startsWith("JOIN")) {
            String code = trimmed.substring(4).trim();
            return reply(phone, joinDistributor(phone, code));
        }

        Retailer retailer = resolveRetailer(phone);

        // New customer: not linked to any distributor → guided registration + seller approval.
        if (retailer == null || retailer.getDistributorId() == null) {
            if (supplierContextId != null) {
                return reply(phone, conversationService.handleRegistration(supplierContextId, phone, trimmed));
            }
            return reply(phone, "Welcome! To start ordering, send: JOIN <your distributor code>.");
        }

        // Linked retailer → stateful conversation engine.
        return reply(phone, conversationService.handle(retailer.getDistributorId(), retailer, phone, trimmed));
    }

    // ---------------- onboarding ----------------

    private String joinDistributor(String phone, String code) {
        if (code.isBlank()) {
            return "Please send: JOIN <your distributor code>";
        }
        Supplier distributor = supplierRepository.findByInviteCode(code).orElse(null);
        if (distributor == null) {
            return "Invalid distributor code. Please check and try again.";
        }
        User user = findOrCreateRetailerUser(phone);
        Retailer retailer = retailerRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Retailer r = new Retailer();
                    r.setUserId(user.getId());
                    r.setShopName(user.getName());
                    return r;
                });
        retailer.setDistributorId(distributor.getId());
        retailerRepository.save(retailer);
        return "You are now connected to " + distributor.getBusinessName()
                + ". Send your order any time, e.g. \"2 Maggi, 5 Parle-G\", or type *menu*.";
    }

    // ---------------- helpers ----------------

    private WhatsAppSettings resolveSettings(String toPhone) {
        if (toPhone == null || toPhone.isBlank()) return null;
        return settingsRepo.findByBusinessNumberAndConnectedTrue(normalizePhone(toPhone)).orElse(null);
    }

    /** True if the current IST time is within the distributor's configured hours. */
    private boolean withinBusinessHours(WhatsAppSettings s) {
        try {
            java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"));
            java.time.LocalTime start = java.time.LocalTime.parse(s.getBusinessHoursStart());
            java.time.LocalTime end = java.time.LocalTime.parse(s.getBusinessHoursEnd());
            if (start.equals(end)) return true; // treat equal start/end as 24h
            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            }
            return !now.isBefore(start) || now.isBefore(end); // overnight window
        } catch (Exception e) {
            return true; // never block on a parse error
        }
    }

    private Retailer resolveRetailer(String phone) {
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user != null) {
            Retailer r = retailerRepository.findByUserId(user.getId()).orElse(null);
            if (r != null) return r;
        }
        return phoneRepo.findByPhone(phone)
                .flatMap(rp -> retailerRepository.findById(rp.getRetailerId()))
                .orElse(null);
    }

    private User findOrCreateRetailerUser(String phone) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            Role role = roleRepository.findByName(RoleName.RETAILER)
                    .orElseThrow(() -> new BusinessException("ROLE_MISSING", HttpStatus.INTERNAL_SERVER_ERROR,
                            "Retailer role missing"));
            User u = new User();
            u.setName("WhatsApp Retailer");
            u.setPhone(phone);
            u.setStatus(Enums.UserStatus.ACTIVE);
            u.setPhoneVerified(true);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });
    }

    private String reply(String phone, String message) {
        if (phone != null && whatsAppClient.isEnabled()) {
            whatsAppClient.sendText(phone, message);
        }
        return message;
    }

    private String normalizePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() > 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }
}
