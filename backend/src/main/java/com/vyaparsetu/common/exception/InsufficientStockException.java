package com.vyaparsetu.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BaseException {
    public InsufficientStockException(String message) {
        super("INSUFFICIENT_STOCK", HttpStatus.CONFLICT, message);
    }
}
