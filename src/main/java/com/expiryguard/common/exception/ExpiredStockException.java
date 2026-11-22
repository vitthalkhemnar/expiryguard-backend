package com.expiryguard.common.exception;

public class ExpiredStockException extends RuntimeException {
    public ExpiredStockException(String message) {
        super(message);
    }
}
