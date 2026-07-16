package com.vyaparsetu.whatsapp.service;

import com.vyaparsetu.auth.service.OtpService;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.user.entity.Retailer;
import com.vyaparsetu.user.entity.Role;
import com.vyaparsetu.user.entity.Supplier;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.RetailerRepository;
import com.vyaparsetu.user.repository.RoleRepository;
import com.vyaparsetu.user.repository.SupplierRepository;
import com.vyaparsetu.user.repository.UserRepository;
import com.vyaparsetu.user.service.UserService;
import com.vyaparsetu.notification.whatsapp.WhatsAppService;
import com.vyaparsetu.whatsapp.WhatsAppEnums;
import com.vyaparsetu.whatsapp.dto.WhatsAppDtos;
import com.vyaparsetu.whatsapp.entity.ProductAlias;
import com.vyaparsetu.whatsapp.entity.RetailerPhone;
import com.vyaparsetu.whatsapp.entity.RetailerRequest;
import com.vyaparsetu.whatsapp.entity.WhatsAppSettings;
import com.vyaparsetu.whatsapp.repository.ProductAliasRepository;
import com.vyaparsetu.whatsapp.repository.RetailerPhoneRepository;
import com.vyaparsetu.whatsapp.repository.RetailerRequestRepository;
import com.vyaparsetu.whatsapp.repository.WhatsAppSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Distributor-facing management of the WhatsApp AI assistant: connection,
 * AI settings, human takeover and customer onboarding approvals.
 */
@Service
public class WhatsAppAdminService {

    private final WhatsAppSettingsRepository settingsRepo;
    private final RetailerRequestRepository requestRepo;
    private final RetailerPhoneRepository phoneRepo;
    private final SupplierRepository supplierRepo;
    private final RetailerRepository retailerRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final OtpService otpService;
    private final UserService userService;
    private final WhatsAppService whatsAppService;
    private final WhatsAppConversationService conversationService;
    private final ProductAliasRepository aliasRepo;

    public WhatsAppAdminService(WhatsAppSettingsRepository settingsRepo, RetailerRequestRepository requestRepo,
                                RetailerPhoneRepository phoneRepo, SupplierRepository supplierRepo,
                                RetailerRepository retailerRepo, UserRepository userRepo, RoleRepository roleRepo,
                                OtpService otpService, UserService userService, WhatsAppService whatsAppService,
                                WhatsAppConversationService conversationService, ProductAliasRepository aliasRepo) {
        this.settingsRepo = settingsRepo;
        this.requestRepo = requestRepo;
        this.phoneRepo = phoneRepo;
        this.supplierRepo = supplierRepo;
        this.retailerRepo = retailerRepo;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.otpService = otpService;
        this.userService = userService;
        this.whatsAppService = whatsAppService;
        this.conversationService = conversationService;
        this.aliasRepo = aliasRepo;
    }

    // ---------------- settings ----------------

    @Transactional
    public WhatsAppSettings settingsFor(Long supplierId) {
        return settingsRepo.findBySupplierId(supplierId).orElseGet(() -> {
            WhatsAppSettings s = new WhatsAppSettings();
            s.setSupplierId(supplierId);
            return settingsRepo.save(s);
        });
    }

    @Transactional
    public WhatsAppDtos.SettingsResponse getSettings() {
        return WhatsAppDtos.SettingsResponse.from(settingsFor(userService.currentSupplierId()));
    }

    @Transactional
    public WhatsAppDtos.SettingsResponse updateSettings(WhatsAppDtos.SettingsUpdateRequest req) {
        WhatsAppSettings s = settingsFor(userService.currentSupplierId());
        if (req.aiEnabled() != null) s.setAiEnabled(req.aiEnabled());
        if (req.autoReply() != null) s.setAutoReply(req.autoReply());
        if (req.autoCreateOrders() != null) s.setAutoCreateOrders(req.autoCreateOrders());
        if (req.requireConfirmation() != null) s.setRequireConfirmation(req.requireConfirmation());
        if (req.sellerApprovalMode() != null) s.setSellerApprovalMode(req.sellerApprovalMode());
        if (req.language() != null) s.setLanguage(req.language());
        if (req.businessHoursStart() != null) s.setBusinessHoursStart(req.businessHoursStart());
        if (req.businessHoursEnd() != null) s.setBusinessHoursEnd(req.businessHoursEnd());
        return WhatsAppDtos.SettingsResponse.from(settingsRepo.save(s));
    }

    @Transactional
    public WhatsAppDtos.SettingsResponse connect(String businessNumber) {
        Long supplierId = userService.currentSupplierId();
        String normalized = normalizePhone(businessNumber);
        WhatsAppSettings s = settingsFor(supplierId);
        s.setBusinessNumber(normalized);
        s.setConnected(true);
        settingsRepo.save(s);
        // keep the supplier profile in sync so invite links / ordering work
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
        supplier.setWhatsappNumber(normalized);
        supplier.setWhatsappEnabled(true);
        supplierRepo.save(supplier);
        return WhatsAppDtos.SettingsResponse.from(s);
    }

    @Transactional
    public WhatsAppDtos.SettingsResponse disconnect() {
        Long supplierId = userService.currentSupplierId();
        WhatsAppSettings s = settingsFor(supplierId);
        s.setConnected(false);
        settingsRepo.save(s);
        supplierRepo.findById(supplierId).ifPresent(sup -> {
            sup.setWhatsappEnabled(false);
            supplierRepo.save(sup);
        });
        return WhatsAppDtos.SettingsResponse.from(s);
    }

    @Transactional
    public WhatsAppDtos.SettingsResponse setTakeover(boolean enabled) {
        WhatsAppSettings s = settingsFor(userService.currentSupplierId());
        s.setHumanTakeover(enabled);
        return WhatsAppDtos.SettingsResponse.from(settingsRepo.save(s));
    }

    /** Run the assistant against a simulated inbound message for the current distributor. */
    @Transactional
    public String simulate(String fromPhone, String text) {
        Long supplierId = userService.currentSupplierId();
        settingsFor(supplierId); // ensure a settings row exists so toggles apply
        return whatsAppService.simulateInbound(supplierId, fromPhone, text);
    }

    // ---------------- onboarding requests ----------------

    @Transactional(readOnly = true)
    public List<WhatsAppDtos.RetailerRequestResponse> listRequests(WhatsAppEnums.RequestStatus status) {
        Long supplierId = userService.currentSupplierId();
        List<RetailerRequest> rows = (status == null)
                ? requestRepo.findBySupplierIdOrderByIdDesc(supplierId)
                : requestRepo.findBySupplierIdAndStatusOrderByIdDesc(supplierId, status);
        return rows.stream().map(WhatsAppDtos.RetailerRequestResponse::from).toList();
    }

    @Transactional
    public void approveRequest(Long requestId) {
        Long supplierId = userService.currentSupplierId();
        RetailerRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RetailerRequest", requestId));
        if (!req.getSupplierId().equals(supplierId)) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Not your request");
        }
        if (req.getStatus() != WhatsAppEnums.RequestStatus.PENDING) {
            throw new BusinessException("ALREADY_HANDLED", HttpStatus.CONFLICT, "Request already handled");
        }

        String phone = normalizePhone(req.getPhone());
        User user = userRepo.findByPhone(phone).orElseGet(() -> {
            Role role = roleRepo.findByName(RoleName.RETAILER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", RoleName.RETAILER));
            User u = new User();
            u.setName(req.getOwnerName() != null ? req.getOwnerName() : "WhatsApp Retailer");
            u.setPhone(phone);
            u.setStatus(Enums.UserStatus.PENDING);
            u.setRoles(Set.of(role));
            return userRepo.save(u);
        });

        Retailer retailer = retailerRepo.findByUserId(user.getId()).orElseGet(() -> {
            Retailer r = new Retailer();
            r.setUserId(user.getId());
            r.setShopName(req.getShopName() != null ? req.getShopName() : user.getName());
            return r;
        });
        retailer.setDistributorId(supplierId);
        if (req.getGstNumber() != null) retailer.setGstNumber(req.getGstNumber());
        if (req.getAddress() != null) retailer.setAddress(req.getAddress());
        retailer = retailerRepo.save(retailer);

        if (!phoneRepo.existsByPhone(phone)) {
            RetailerPhone rp = new RetailerPhone();
            rp.setRetailerId(retailer.getId());
            rp.setPhone(phone);
            rp.setVerified(true); // proven via WhatsApp
            phoneRepo.save(rp);
        }

        // login OTP so the new retailer can activate the app account
        otpService.generateAndSend(phone, Enums.OtpChannel.SMS, Enums.OtpPurpose.LOGIN, user.getId());

        req.setStatus(WhatsAppEnums.RequestStatus.APPROVED);
        requestRepo.save(req);

        // reset any pending registration conversation so the retailer can order immediately
        conversationService.clearSession(supplierId, phone);
    }

    @Transactional
    public void rejectRequest(Long requestId) {
        Long supplierId = userService.currentSupplierId();
        RetailerRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RetailerRequest", requestId));
        if (!req.getSupplierId().equals(supplierId)) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Not your request");
        }
        req.setStatus(WhatsAppEnums.RequestStatus.REJECTED);
        requestRepo.save(req);
    }

    // ---------------- linked numbers ----------------

    @Transactional(readOnly = true)
    public List<WhatsAppDtos.LinkedNumberResponse> linkedNumbers(Long retailerId) {
        return phoneRepo.findByRetailerId(retailerId).stream()
                .map(p -> new WhatsAppDtos.LinkedNumberResponse(p.getId(), p.getPhone(), p.isVerified()))
                .toList();
    }

    /**
     * Link an additional number to a retailer and send an OTP to it. If the number
     * is already linked to this retailer but unverified, the OTP is simply re-sent.
     */
    @Transactional
    public WhatsAppDtos.LinkedNumberResponse addNumber(Long retailerId, String phone) {
        Retailer retailer = ownedRetailer(retailerId);
        String normalized = normalizePhone(phone);

        RetailerPhone existing = phoneRepo.findByPhone(normalized).orElse(null);
        if (existing != null) {
            if (!existing.getRetailerId().equals(retailer.getId()) || existing.isVerified()) {
                throw new BusinessException("PHONE_TAKEN", HttpStatus.CONFLICT, "Number already linked");
            }
            // unverified + same retailer: resend OTP
            otpService.generateAndSend(normalized, Enums.OtpChannel.SMS, Enums.OtpPurpose.REGISTER, null);
            return new WhatsAppDtos.LinkedNumberResponse(existing.getId(), existing.getPhone(), existing.isVerified());
        }

        RetailerPhone rp = new RetailerPhone();
        rp.setRetailerId(retailerId);
        rp.setPhone(normalized);
        rp.setVerified(false);
        rp = phoneRepo.save(rp);
        otpService.generateAndSend(normalized, Enums.OtpChannel.SMS, Enums.OtpPurpose.REGISTER, null);
        return new WhatsAppDtos.LinkedNumberResponse(rp.getId(), rp.getPhone(), rp.isVerified());
    }

    /** Verify a linked number with the OTP sent to it. */
    @Transactional
    public WhatsAppDtos.LinkedNumberResponse verifyNumber(Long retailerId, String phone, String code) {
        ownedRetailer(retailerId);
        String normalized = normalizePhone(phone);
        RetailerPhone rp = phoneRepo.findByPhone(normalized)
                .filter(p -> p.getRetailerId().equals(retailerId))
                .orElseThrow(() -> new ResourceNotFoundException("Linked number", normalized));
        otpService.verify(normalized, code, Enums.OtpPurpose.REGISTER);
        rp.setVerified(true);
        rp = phoneRepo.save(rp);
        return new WhatsAppDtos.LinkedNumberResponse(rp.getId(), rp.getPhone(), rp.isVerified());
    }

    @Transactional
    public void removeNumber(Long retailerId, Long numberId) {
        ownedRetailer(retailerId);
        RetailerPhone rp = phoneRepo.findById(numberId)
                .filter(p -> p.getRetailerId().equals(retailerId))
                .orElseThrow(() -> new ResourceNotFoundException("Linked number", numberId));
        phoneRepo.delete(rp);
    }

    private Retailer ownedRetailer(Long retailerId) {
        Long supplierId = userService.currentSupplierId();
        Retailer retailer = retailerRepo.findById(retailerId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));
        if (!supplierId.equals(retailer.getDistributorId())) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Not your retailer");
        }
        return retailer;
    }

    // ---------------- product aliases ----------------

    @Transactional(readOnly = true)
    public List<WhatsAppDtos.AliasResponse> listAliases() {
        return aliasRepo.findBySupplierId(userService.currentSupplierId()).stream()
                .map(a -> new WhatsAppDtos.AliasResponse(a.getId(), a.getAlias(), a.getProductId(), a.getCanonical()))
                .toList();
    }

    @Transactional
    public WhatsAppDtos.AliasResponse addAlias(WhatsAppDtos.AddAliasRequest req) {
        Long sid = userService.currentSupplierId();
        if (aliasRepo.existsBySupplierIdAndAliasIgnoreCase(sid, req.alias().trim())) {
            throw new BusinessException("ALIAS_EXISTS", HttpStatus.CONFLICT, "That alias already exists");
        }
        ProductAlias a = new ProductAlias();
        a.setSupplierId(sid);
        a.setAlias(req.alias().trim());
        a.setProductId(req.productId());
        a.setCanonical(req.canonical());
        a = aliasRepo.save(a);
        return new WhatsAppDtos.AliasResponse(a.getId(), a.getAlias(), a.getProductId(), a.getCanonical());
    }

    @Transactional
    public void deleteAlias(Long id) {
        Long sid = userService.currentSupplierId();
        aliasRepo.findById(id).filter(a -> a.getSupplierId().equals(sid)).ifPresent(aliasRepo::delete);
    }

    private String normalizePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) return digits.substring(2);
        if (digits.length() > 10) return digits.substring(digits.length() - 10);
        return digits;
    }
}
