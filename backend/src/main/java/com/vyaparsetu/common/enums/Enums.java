package com.vyaparsetu.common.enums;

/**
 * Container for the small shared enums used across modules.
 * Larger domain enums (OrderStatus, etc.) live in their own files.
 */
public final class Enums {

    private Enums() {
    }

    public enum Language { hi, en }

    public enum UserStatus { ACTIVE, SUSPENDED, PENDING }

    public enum OtpChannel { SMS, EMAIL }

    public enum OtpPurpose { LOGIN, REGISTER, RESET }

    public enum SupplierType { DISTRIBUTOR, WHOLESALER, SUPER_STOCKIST }

    public enum MovementType { PURCHASE, SALE, RETURN, ADJUSTMENT, EXPIRY }

    public enum PaymentMode { UPI, CARD, NET_BANKING, WALLET, COD, CREDIT, ADVANCE }

    public enum PaymentStatus { PENDING, PAID, PARTIAL, REFUNDED, FAILED }

    public enum OrderSource { CART, REPEAT, VOICE, TEXT, IMAGE, BARCODE, AI }

    public enum NotificationType {
        LOW_STOCK, OFFER, PAYMENT_REMINDER, DELIVERY_UPDATE, AI_RECOMMENDATION, ORDER_UPDATE, SYSTEM,
        // Distributor-facing operational alerts
        NEW_RETAILER, ORDER_MODIFIED, LARGE_ORDER, CREDIT_EXCEEDED, AI_EXTRACTION_FAILED,
        // Retailer-facing
        REGISTRATION_APPROVED
    }

    public enum NotificationChannel { PUSH, IN_APP, EMAIL, WHATSAPP, SMS }
}
