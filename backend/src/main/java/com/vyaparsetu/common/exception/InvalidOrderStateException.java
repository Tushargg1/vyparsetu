package com.vyaparsetu.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends BaseException {
    public InvalidOrderStateException(String message) {
        super("INVALID_STATE", HttpStatus.CONFLICT, message);
    }
}
