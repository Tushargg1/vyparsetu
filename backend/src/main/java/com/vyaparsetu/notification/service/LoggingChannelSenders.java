package com.vyaparsetu.notification.service;

import com.vyaparsetu.common.enums.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default logging implementations for outbound channels. Replace with real
 * provider integrations (FCM, SMTP, SMS gateway) in production. WhatsApp has its
 * own gated client in the notification.whatsapp package.
 */
public class LoggingChannelSenders {

    private static final Logger log = LoggerFactory.getLogger(LoggingChannelSenders.class);

    @Component
    public static class PushSender implements NotificationChannelSender {
        public Enums.NotificationChannel channel() { return Enums.NotificationChannel.PUSH; }
        public void send(String recipient, String title, String body) {
            log.info("[PUSH] to {} : {} - {}", recipient, title, body);
        }
    }

    @Component
    public static class EmailSender implements NotificationChannelSender {
        public Enums.NotificationChannel channel() { return Enums.NotificationChannel.EMAIL; }
        public void send(String recipient, String title, String body) {
            log.info("[EMAIL] to {} : {} - {}", recipient, title, body);
        }
    }

    @Component
    public static class SmsSender implements NotificationChannelSender {
        public Enums.NotificationChannel channel() { return Enums.NotificationChannel.SMS; }
        public void send(String recipient, String title, String body) {
            log.info("[SMS] to {} : {} - {}", recipient, title, body);
        }
    }
}
