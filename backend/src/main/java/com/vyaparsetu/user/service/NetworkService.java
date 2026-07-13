package com.vyaparsetu.user.service;

import com.vyaparsetu.auth.service.OtpService;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.order.repository.OrderRepository;
import com.vyaparsetu.user.dto.NetworkDtos;
import com.vyaparsetu.user.entity.Retailer;
import com.vyaparsetu.user.entity.Role;
import com.vyaparsetu.user.entity.Supplier;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.RetailerRepository;
import com.vyaparsetu.user.repository.RoleRepository;
import com.vyaparsetu.user.repository.SupplierRepository;
import com.vyaparsetu.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the distributor<->retailer network: invite codes, linking,
 * a distributor's retailer roster, and WhatsApp settings.
 */
@Service
public class NetworkService {

    private final SupplierRepository supplierRepository;
    private final RetailerRepository retailerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpService otpService;
    private final UserService userService;
    private final OrderRepository orderRepository;

    public NetworkService(SupplierRepository supplierRepository, RetailerRepository retailerRepository,
                          UserRepository userRepository, RoleRepository roleRepository,
                          OtpService otpService, UserService userService, OrderRepository orderRepository) {
        this.supplierRepository = supplierRepository;
        this.retailerRepository = retailerRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.otpService = otpService;
        this.userService = userService;
        this.orderRepository = orderRepository;
    }

    /** All distributors a retailer can browse and order from. */
    @Transactional(readOnly = true)
    public List<NetworkDtos.DistributorResponse> listDistributors() {
        return supplierRepository.findAll().stream()
                .map(this::toDistributorResponse)
                .toList();
    }

    // ---------- Distributor (supplier) side ----------

    @Transactional(readOnly = true)
    public NetworkDtos.InviteCodeResponse myInviteCode() {
        Supplier supplier = currentSupplier();
        String link = (supplier.getWhatsappNumber() != null)
                ? "https://wa.me/" + supplier.getWhatsappNumber()
                + "?text=" + urlEncode("JOIN " + supplier.getInviteCode())
                : null;
        return new NetworkDtos.InviteCodeResponse(supplier.getInviteCode(), link);
    }

    @Transactional(readOnly = true)
    public List<NetworkDtos.RetailerSummary> myRetailers() {
        Long supplierId = userService.currentSupplierId();
        // Retailers linked via invite code, plus anyone who has ordered from this distributor.
        Set<Long> retailerIds = new LinkedHashSet<>();
        List<Retailer> linked = retailerRepository.findByDistributorId(supplierId);
        linked.forEach(r -> retailerIds.add(r.getId()));
        retailerIds.addAll(orderRepository.findDistinctRetailerIdsBySupplierId(supplierId));

        List<Retailer> retailers = retailerRepository.findAllById(retailerIds);
        Map<Long, User> users = userRepository.findAllById(
                        retailers.stream().map(Retailer::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<NetworkDtos.RetailerSummary> out = new ArrayList<>();
        for (Retailer r : retailers) {
            User u = users.get(r.getUserId());
            out.add(toRetailerSummary(r, u));
        }
        return out;
    }

    private NetworkDtos.RetailerSummary toRetailerSummary(Retailer r, User u) {
        return new NetworkDtos.RetailerSummary(r.getId(), r.getShopName(),
                u != null ? u.getName() : null, u != null ? u.getPhone() : null,
                r.getCity(), r.getAddress(), r.getState(), r.getPincode(),
                r.getAltPhones(), r.getLocationUrl(), r.isCreditApproved());
    }

    @Transactional
    public NetworkDtos.RetailerSummary addRetailer(NetworkDtos.AddRetailerRequest req) {
        Supplier supplier = currentSupplier();
        if (userRepository.existsByPhone(req.phone())) {
            throw new BusinessException("PHONE_TAKEN", HttpStatus.CONFLICT, "Phone already registered");
        }
        Role role = roleRepository.findByName(RoleName.RETAILER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", RoleName.RETAILER));

        User user = new User();
        user.setName(req.name());
        user.setPhone(req.phone());
        user.setStatus(Enums.UserStatus.PENDING);
        user.setRoles(Set.of(role));
        user = userRepository.save(user);

        Retailer retailer = new Retailer();
        retailer.setUserId(user.getId());
        retailer.setShopName(req.shopName() != null ? req.shopName() : req.name());
        retailer.setCity(req.city());
        retailer.setAddress(req.address());
        retailer.setAltPhones(req.altPhones());
        retailer.setLocationUrl(req.locationUrl());
        retailer.setDistributorId(supplier.getId());
        retailer = retailerRepository.save(retailer);

        // send a login OTP so the retailer can activate their account
        otpService.generateAndSend(req.phone(), Enums.OtpChannel.SMS, Enums.OtpPurpose.LOGIN, user.getId());

        return toRetailerSummary(retailer, user);
    }

    @Transactional
    public void setWhatsApp(NetworkDtos.WhatsAppSettingsRequest req) {
        Supplier supplier = currentSupplier();
        supplier.setWhatsappNumber(req.whatsappNumber());
        supplier.setWhatsappEnabled(req.enabled());
        supplierRepository.save(supplier);
    }

    /**
     * Records a payment received from a retailer, applying it to that retailer's
     * outstanding orders with this distributor, oldest first.
     */
    @Transactional
    public NetworkDtos.RecordPaymentResult recordRetailerPayment(Long retailerId, java.math.BigDecimal amount) {
        Long supplierId = userService.currentSupplierId();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("BAD_AMOUNT", HttpStatus.BAD_REQUEST, "Enter a valid amount");
        }
        java.time.Instant now = java.time.Instant.now();
        java.math.BigDecimal remaining = amount;
        int settled = 0;

        var orders = orderRepository.findByRetailerIdAndSupplierIdOrderByPlacedAtAsc(retailerId, supplierId);
        for (com.vyaparsetu.order.entity.Order o : orders) {
            if (remaining.signum() <= 0) break;
            if (o.getStatus() == com.vyaparsetu.order.entity.OrderStatus.REJECTED
                    || o.getStatus() == com.vyaparsetu.order.entity.OrderStatus.CANCELLED) continue;

            java.math.BigDecimal paid = o.getAmountPaid() == null ? java.math.BigDecimal.ZERO : o.getAmountPaid();
            java.math.BigDecimal due = o.getTotalAmount().subtract(paid);
            if (due.signum() <= 0) continue;

            java.math.BigDecimal pay = remaining.min(due);
            o.setAmountPaid(paid.add(pay));
            o.setLastPaymentAt(now);
            o.setPaymentStatus(o.getAmountPaid().compareTo(o.getTotalAmount()) >= 0
                    ? Enums.PaymentStatus.PAID : Enums.PaymentStatus.PARTIAL);
            orderRepository.save(o);
            remaining = remaining.subtract(pay);
            if (o.getPaymentStatus() == Enums.PaymentStatus.PAID) settled++;
        }

        // Remaining outstanding due across this retailer's live orders.
        java.math.BigDecimal outstanding = java.math.BigDecimal.ZERO;
        for (com.vyaparsetu.order.entity.Order o : orders) {
            if (o.getStatus() == com.vyaparsetu.order.entity.OrderStatus.REJECTED
                    || o.getStatus() == com.vyaparsetu.order.entity.OrderStatus.CANCELLED) continue;
            java.math.BigDecimal paid = o.getAmountPaid() == null ? java.math.BigDecimal.ZERO : o.getAmountPaid();
            java.math.BigDecimal due = o.getTotalAmount().subtract(paid);
            if (due.signum() > 0) outstanding = outstanding.add(due);
        }

        return new NetworkDtos.RecordPaymentResult(amount.subtract(remaining), remaining, settled, outstanding);
    }

    // ---------- Retailer side ----------

    @Transactional
    public NetworkDtos.DistributorResponse joinByCode(String inviteCode) {
        Retailer retailer = userService.currentRetailer();
        if (retailer.getDistributorId() != null) {
            throw new BusinessException("ALREADY_LINKED", HttpStatus.CONFLICT,
                    "You are already linked to a distributor");
        }
        Supplier distributor = supplierRepository.findByInviteCode(inviteCode.trim())
                .orElseThrow(() -> new BusinessException("INVALID_INVITE_CODE", HttpStatus.BAD_REQUEST,
                        "Invalid distributor invite code"));
        retailer.setDistributorId(distributor.getId());
        retailerRepository.save(retailer);
        return toDistributorResponse(distributor);
    }

    @Transactional(readOnly = true)
    public NetworkDtos.DistributorResponse myDistributor() {
        Long distributorId = userService.currentDistributorId();
        Supplier distributor = supplierRepository.findById(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", distributorId));
        return toDistributorResponse(distributor);
    }

    // ---------- Self-service profile (both roles) ----------

    @Transactional(readOnly = true)
    public NetworkDtos.MyProfileResponse mySupplierProfile() {
        Supplier s = currentSupplier();
        User u = userRepository.findById(s.getUserId()).orElse(null);
        return new NetworkDtos.MyProfileResponse(
                u != null ? u.getName() : null, u != null ? u.getPhone() : null,
                s.getBusinessName(), s.getAddress(), s.getCity(), s.getState(), s.getPincode(),
                s.getAltPhones(), s.getLocationUrl(), s.getInviteCode(), s.getWhatsappNumber());
    }

    @Transactional
    public NetworkDtos.MyProfileResponse updateSupplierProfile(NetworkDtos.ProfileRequest req) {
        Supplier s = currentSupplier();
        if (req.displayName() != null && !req.displayName().isBlank()) s.setBusinessName(req.displayName().trim());
        s.setAddress(req.address());
        s.setCity(req.city());
        s.setState(req.state());
        s.setPincode(req.pincode());
        s.setAltPhones(req.altPhones());
        s.setLocationUrl(req.locationUrl());
        supplierRepository.save(s);
        updateOwnerName(s.getUserId(), req.ownerName());
        return mySupplierProfile();
    }

    @Transactional(readOnly = true)
    public NetworkDtos.MyProfileResponse myRetailerProfile() {
        Retailer r = userService.currentRetailer();
        User u = userRepository.findById(r.getUserId()).orElse(null);
        return new NetworkDtos.MyProfileResponse(
                u != null ? u.getName() : null, u != null ? u.getPhone() : null,
                r.getShopName(), r.getAddress(), r.getCity(), r.getState(), r.getPincode(),
                r.getAltPhones(), r.getLocationUrl(), null, null);
    }

    @Transactional
    public NetworkDtos.MyProfileResponse updateRetailerProfile(NetworkDtos.ProfileRequest req) {
        Retailer r = userService.currentRetailer();
        if (req.displayName() != null && !req.displayName().isBlank()) r.setShopName(req.displayName().trim());
        r.setAddress(req.address());
        r.setCity(req.city());
        r.setState(req.state());
        r.setPincode(req.pincode());
        r.setAltPhones(req.altPhones());
        r.setLocationUrl(req.locationUrl());
        retailerRepository.save(r);
        updateOwnerName(r.getUserId(), req.ownerName());
        return myRetailerProfile();
    }

    private void updateOwnerName(Long userId, String ownerName) {
        if (ownerName == null || ownerName.isBlank()) return;
        userRepository.findById(userId).ifPresent(u -> {
            u.setName(ownerName.trim());
            userRepository.save(u);
        });
    }

    // ---------- helpers ----------

    private Supplier currentSupplier() {
        return supplierRepository.findById(userService.currentSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "current"));
    }

    private NetworkDtos.DistributorResponse toDistributorResponse(Supplier s) {
        User owner = userRepository.findById(s.getUserId()).orElse(null);
        return new NetworkDtos.DistributorResponse(s.getId(), s.getBusinessName(), s.getSupplierType(),
                owner != null ? owner.getName() : null, owner != null ? owner.getPhone() : null,
                s.getCity(), s.getState(), s.getAddress(), s.getPincode(),
                s.getAltPhones(), s.getLocationUrl(), s.getWhatsappNumber(), s.isWhatsappEnabled());
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
