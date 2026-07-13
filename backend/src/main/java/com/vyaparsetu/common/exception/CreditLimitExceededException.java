package com.vyaparsetu.common.exception;

import org.springframework.http.HttpStatus;

public class CreditLimitExceededException extends BaseException {
    public CreditLimitExceededException(String message) {
        super("CREDIT_LIMIT_EXCEEDED", HttpStatus.CONFLICT, message);
    }
}
