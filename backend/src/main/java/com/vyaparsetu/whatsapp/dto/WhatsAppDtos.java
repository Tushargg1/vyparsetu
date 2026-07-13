package com.vyaparsetu.whatsapp.dto;

import com.vyaparsetu.whatsapp.WhatsAppEnums;
import com.vyaparsetu.whatsapp.entity.RetailerRequest;
import com.vyaparsetu.whatsapp.entity.WhatsAppSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public final class WhatsAppDtos {

    private WhatsAppDtos() {
    }

    public record SettingsResponse(
            boolean connected,
            String businessNumber,
            boolean aiEnabled,
            boolean autoReply,
            boolean autoCreateOrders,
            boolean requireConfirmation,
            boolean humanTakeover,
            WhatsAppEnums.SellerApprovalMode sellerApprovalMode,
            WhatsAppEnums.ChatLanguage language,
            String businessHoursStart,
            String businessHoursEnd
    ) {
        public static SettingsResponse from(WhatsAppSettings s) {
            return new SettingsResponse(s.isConnected(), s.getBusinessNumber(), s.isAiEnabled(),
                    s.isAutoReply(), s.isAutoCreateOrders(), s.isRequireConfirmation(), s.isHumanTakeover(),
                    s.getSellerApprovalMode(), s.getLanguage(), s.getBusinessHoursStart(), s.getBusinessHoursEnd());
        }
    }

    /** All fields optional; only non-null values are applied (partial update). */
    public record SettingsUpdateRequest(
            Boolean aiEnabled,
            Boolean autoReply,
            Boolean autoCreateOrders,
            Boolean requireConfirmation,
            WhatsAppEnums.SellerApprovalMode sellerApprovalMode,
            WhatsAppEnums.ChatLanguage language,
            String businessHoursStart,
            String businessHoursEnd
    ) {
    }

    public record ConnectRequest(
            @NotBlank @Pattern(regexp = "^\\d{10,15}$", message = "Invalid WhatsApp number") String businessNumber
    ) {
    }

    public record TakeoverRequest(boolean enabled) {
    }

    public record RetailerRequestResponse(
            Long id,
            String shopName,
            String gstNumber,
            String address,
            String ownerName,
            String phone,
            WhatsAppEnums.RequestStatus status,
            String message,
            Instant createdAt
    ) {
        public static RetailerRequestResponse from(RetailerRequest r) {
            return new RetailerRequestResponse(r.getId(), r.getShopName(), r.getGstNumber(), r.getAddress(),
                    r.getOwnerName(), r.getPhone(), r.getStatus(), r.getMessage(), r.getCreatedAt());
        }
    }

    public record LinkedNumberResponse(Long id, String phone, boolean verified) {
    }

    public record AddNumberRequest(
            @NotBlank @Pattern(regexp = "^\\d{10,15}$", message = "Invalid phone number") String phone
    ) {
    }

    public record VerifyNumberRequest(
            @NotBlank @Pattern(regexp = "^\\d{10,15}$", message = "Invalid phone number") String phone,
            @NotBlank String code
    ) {
    }

    public record SimulateRequest(
            @NotBlank @Pattern(regexp = "^\\d{10,15}$", message = "Invalid phone number") String from,
            @NotBlank String text
    ) {
    }

    public record AliasResponse(Long id, String alias, Long productId, String canonical) {
    }

    public record AddAliasRequest(
            @NotBlank String alias,
            Long productId,
            String canonical
    ) {
    }
}
