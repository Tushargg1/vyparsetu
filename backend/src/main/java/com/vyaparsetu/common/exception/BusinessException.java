package com.vyaparsetu.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Generic business-rule violation. Use a specific subclass when one exists.
 */
public class BusinessException extends BaseException {
    public BusinessException(String code, HttpStatus status, String message) {
        super(code, status, message);
    }

    public BusinessException(String message) {
        super("BUSINESS_ERROR", HttpStatus.CONFLICT, message);
    }
}
