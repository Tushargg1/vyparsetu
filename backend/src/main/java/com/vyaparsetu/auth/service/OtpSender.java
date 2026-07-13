package com.vyaparsetu.auth.service;

import com.vyaparsetu.common.enums.Enums;

/**
 * Abstraction over OTP delivery channels (SMS / Email).
 * Swappable: dev logs the code, prod integrates a real provider.
 */
public interface OtpSender {
    Enums.OtpChannel channel();

    void send(String identifier, String code);
}
