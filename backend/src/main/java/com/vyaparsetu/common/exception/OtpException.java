package com.vyaparsetu.common.exception;

import org.springframework.http.HttpStatus;

public class OtpException extends BaseException {
    public OtpException(String message) {
        super("OTP_INVALID", HttpStatus.BAD_REQUEST, message);
    }

    public OtpException(String code, HttpStatus status, String message) {
        super(code, status, message);
    }
}
