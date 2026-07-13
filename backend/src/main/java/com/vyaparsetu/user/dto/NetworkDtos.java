package com.vyaparsetu.user.dto;

import com.vyaparsetu.common.enums.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class NetworkDtos {

    private NetworkDtos() {
    }

    public record InviteCodeResponse(String inviteCode, String whatsappJoinLink) {
    }

    public record JoinRequest(@NotBlank String inviteCode) {
    }

    public record AddRetailerRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number") String phone,
            String shopName,
            String city,
            String address,
            String altPhones,
            String locationUrl
    ) {
    }

    public record WhatsAppSettingsRequest(
            @Pattern(regexp = "^\\d{10,15}$", message = "Invalid WhatsApp number") String whatsappNumber,
            boolean enabled
    ) {
    }

    /** Distributor records a payment received from a retailer (applied oldest-first). */
    public record RecordPaymentRequest(
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive java.math.BigDecimal amount
    ) {
    }

    public record RecordPaymentResult(
            java.math.BigDecimal applied,
            java.math.BigDecimal leftover,
            int ordersSettled,
            java.math.BigDecimal outstandingDue
    ) {
    }

    /** Self-service profile edit for a retailer or distributor. */
    public record ProfileRequest(
            String ownerName,
            String displayName,
            String address,
            String city,
            String state,
            String pincode,
            String altPhones,
            String locationUrl
    ) {
    }

    /** Full profile of the signed-in retailer/distributor, used to prefill the edit form. */
    public record MyProfileResponse(
            String ownerName,
            String phone,
            String displayName,
            String address,
            String city,
            String state,
            String pincode,
            String altPhones,
            String locationUrl,
            String inviteCode,
            String whatsappNumber
    ) {
    }

    public record DistributorResponse(
            Long id,
            String businessName,
            Enums.SupplierType supplierType,
            String ownerName,
            String phone,
            String city,
            String state,
            String address,
            String pincode,
            String altPhones,
            String locationUrl,
            String whatsappNumber,
            boolean whatsappEnabled
    ) {
    }

    public record RetailerSummary(
            Long retailerId,
            String shopName,
            String ownerName,
            String phone,
            String city,
            String address,
            String state,
            String pincode,
            String altPhones,
            String locationUrl,
            boolean creditApproved
    ) {
    }
}
