package com.vyaparsetu.notification.service;

import com.vyaparsetu.common.enums.Enums;

/**
 * Outbound notification channel abstraction (PUSH/EMAIL/WHATSAPP/SMS).
 * IN_APP is handled directly by the NotificationService via WebSocket.
 */
public interface NotificationChannelSender {
    Enums.NotificationChannel channel();

    void send(String recipient, String title, String body);
}
