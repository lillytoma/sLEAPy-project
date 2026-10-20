package com.sleapy.project.exceptions;

public class InsufficientCashException extends OrderValidationException {
    
    public InsufficientCashException(String message) {
        super("INSUFFICIENT_CASH", message);
    }
}
