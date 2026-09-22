package com.sleapy.project.models.enums;

/**
 * Represents the lifecycle states of an order.
 * PENDING: Order created, awaiting processing
 * ACCEPTED: Order validated and queued for execution
 * FILLED: Order executed successfully
 * REJECTED: Order failed validation or execution
 */
public enum OrderStatus {PENDING, ACCEPTED, FILLED, REJECTED};

