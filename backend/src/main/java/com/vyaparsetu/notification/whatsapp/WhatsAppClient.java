package com.vyaparsetu.notification.whatsapp;

/**
 * WhatsApp messaging abstraction. Premium/optional feature, disabled by default
 * (app.features.whatsapp.enabled=false) because messages are billed per send.
 */
public interface WhatsAppClient {

    boolean isEnabled();

    void sendText(String toPhone, String message);

    void sendDocument(String toPhone, String caption, String documentUrl);
}
