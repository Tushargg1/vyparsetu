package com.vyaparsetu.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String entity, Object id) {
        super("NOT_FOUND", HttpStatus.NOT_FOUND, entity + " not found: " + id);
    }
}
