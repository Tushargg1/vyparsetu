package com.vyaparsetu.whatsapp;

/** Enums for the WhatsApp AI feature. */
public final class WhatsAppEnums {

    private WhatsAppEnums() {
    }

    /** When the seller must approve a retailer before their orders are accepted. */
    public enum SellerApprovalMode { FIRST_ORDER_ONLY, ALWAYS, NEVER }

    /** Reply language for the AI assistant. */
    public enum ChatLanguage { HINDI, ENGLISH, BOTH }

    /** Lifecycle of an inbound retailer onboarding request. */
    public enum RequestStatus { PENDING, APPROVED, REJECTED }
}
