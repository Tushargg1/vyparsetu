package com.vyaparsetu.auth.service;

import com.vyaparsetu.common.enums.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Development OTP senders that simply log the code.
 * Replace with real SMS/email provider implementations in production.
 */
public class LoggingOtpSenders {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSenders.class);

    @Component
    @Profile("!prod")
    public static class SmsOtpSender implements OtpSender {
        @Override
        public Enums.OtpChannel channel() {
            return Enums.OtpChannel.SMS;
        }

        @Override
        public void send(String identifier, String code) {
            log.info("[DEV-SMS-OTP] to {} -> {}", identifier, code);
        }
    }

    @Component
    @Profile("!prod")
    public static class EmailOtpSender implements OtpSender {
        @Override
        public Enums.OtpChannel channel() {
            return Enums.OtpChannel.EMAIL;
        }

        @Override
        public void send(String identifier, String code) {
            log.info("[DEV-EMAIL-OTP] to {} -> {}", identifier, code);
        }
    }
}
