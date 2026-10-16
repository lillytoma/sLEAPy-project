package com.sleapy.project.exceptions;

public class OrderValidationException extends RuntimeException {
    private final String rule;

    public OrderValidationException(String rule, String message) {
        super(message);
        this.rule = rule;
    }

    public String getRule() {
        return rule;
    }
}