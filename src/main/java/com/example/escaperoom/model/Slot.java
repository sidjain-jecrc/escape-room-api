package com.example.escaperoom.model;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "slots")
public class Slot {

    @Id
    private Long slotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status;

    @Column(columnDefinition = "BINARY(16)")
    private UUID holdId;

    private Instant holdExpiresAt;

    protected Slot() {}

    public Slot(Long slotId, SlotStatus status) {
        this.slotId = slotId;
        this.status = status;
    }

    public Long getSlotId() { return slotId; }
    public SlotStatus getStatus() { return status; }
    public UUID getHoldId() { return holdId; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }

    public void setStatus(SlotStatus status) { this.status = status; }
    public void setHoldId(UUID holdId) { this.holdId = holdId; }
    public void setHoldExpiresAt(Instant holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
}
