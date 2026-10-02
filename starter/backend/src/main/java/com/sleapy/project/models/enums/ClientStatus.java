package com.sleapy.project.models.enums;

/**
 * Represents the current status of a client account.
 * 
 * OPEN - Client account is active and can perform trading operations
 * CLOSED - The account has been closed by the client and is no longer active
 * BLOCKED - Client account has been blocked and temporarily restricted from trading
 */
public enum ClientStatus {OPEN, CLOSED, BLOCKED}
