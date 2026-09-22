package com.sleapy.project.exceptions;

public class InsufficientCashException extends RuntimeException {
    
    public InsufficientCashException(String message) {
        super(message);
    }
}
