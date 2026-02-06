package com.example.escaperoom.api.dto;

import java.time.Instant;
import java.util.UUID;

public final class HoldResponse {

    private final UUID holdId;
    private final Instant expiresAt;

    public HoldResponse(UUID holdId, Instant expiresAt) {
        this.holdId = holdId;
        this.expiresAt = expiresAt;
    }

    public UUID getHoldId() {
        return holdId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
