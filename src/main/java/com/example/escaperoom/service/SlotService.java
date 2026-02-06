package com.example.escaperoom.service;

import com.example.escaperoom.api.dto.HoldResponse;
import com.example.escaperoom.model.Slot;
import com.example.escaperoom.model.SlotStatus;
import com.example.escaperoom.repo.SlotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.example.escaperoom.service.ApiExceptions.*;

@Service
public class SlotService {

    private final SlotRepository repo;
    private final Clock clock;
    private final Duration holdDuration;

    public SlotService(SlotRepository repo,
                       Clock clock,
                       @Value("${app.hold.duration-seconds:300}") long holdDurationSeconds) {
        this.repo = repo;
        this.clock = clock;
        this.holdDuration = Duration.ofSeconds(holdDurationSeconds);
    }

    @Transactional
    public HoldResponse holdSlot(Long slotId) {
        // Opportunistic cleanup to reduce surprise "slot not available" due to stale holds
        repo.releaseExpired(Instant.now(clock));

        // Validate slot exists (optional, but makes error clearer)
        if (!repo.existsById(slotId)) {
            throw new NotFound("Slot not found: " + slotId);
        }

        UUID holdId = UUID.randomUUID();
        Instant expiresAt = Instant.now(clock).plus(holdDuration);

        int updated = repo.holdSlot(slotId, holdId, expiresAt);
        if (updated == 0) {
            throw new Conflict("Slot is not available");
        }
        return new HoldResponse(holdId, expiresAt);
    }

    @Transactional
    public void confirmHold(UUID holdId) {
        // First, opportunistically cleanup expired holds.
        repo.releaseExpired(Instant.now(clock));

        int updated = repo.confirmHold(holdId, Instant.now(clock));
        if (updated == 0) {
            // Distinguish "already booked" vs "expired" is possible with another query,
            // but for this take-home we keep it simple and correct.
            throw new Gone("Hold is expired or invalid");
        }
    }

    @Transactional
    public void releaseHold(UUID holdId) {
        repo.releaseHold(holdId);
    }

    @Transactional
    public Slot getSlot(Long slotId) {
        return repo.findById(slotId).orElseThrow(() -> new NotFound("Slot not found: " + slotId));
    }
}
