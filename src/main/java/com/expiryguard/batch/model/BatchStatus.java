package com.expiryguard.batch.model;

public enum BatchStatus {
    ACTIVE,         // Healthy, normal selling price
    NEAR_EXPIRY,    // 15 - 30 days remaining, discounted
    CRITICAL,       // 1 - 14 days remaining, deep clearance discount
    EXPIRED,        // Expired, locked from sale, pending disposal
    DISPOSED        // Written off and physically destroyed/removed
}
