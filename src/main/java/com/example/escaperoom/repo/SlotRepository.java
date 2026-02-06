package com.example.escaperoom.repo;

import com.example.escaperoom.model.Slot;
import com.example.escaperoom.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    /**
     * Atomic transition: AVAILABLE -> HELD (only one caller can succeed).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Slot s SET s.status = SlotStatus.HELD, s.holdId = :holdId, s.holdExpiresAt = :expiresAt WHERE s.slotId = :slotId AND s.status = com.example.escaperoom.model.SlotStatus.AVAILABLE")
    int holdSlot(@Param("slotId") Long slotId,
                 @Param("holdId") UUID holdId,
                 @Param("expiresAt") Instant expiresAt);

    /**
     * Atomic transition: HELD -> BOOKED only if hold is still valid.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Slot s SET s.status = SlotStatus.BOOKED WHERE s.holdId = :holdId AND s.status = com.example.escaperoom.model.SlotStatus.HELD AND s.holdExpiresAt > :now")
    int confirmHold(@Param("holdId") UUID holdId,
                    @Param("now") Instant now);

    /**
     * Release a hold (explicit or via expiration cleanup).
     * We allow release regardless of current status; no-op if holdId doesn't match.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Slot s SET s.status = SlotStatus.AVAILABLE, s.holdId = NULL, s.holdExpiresAt = NULL WHERE s.holdId = :holdId AND s.status = com.example.escaperoom.model.SlotStatus.HELD")
    int releaseHold(@Param("holdId") UUID holdId);

    /**
     * Opportunistic cleanup for expired holds. This is a lightweight alternative to a background job.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Slot s SET s.status = SlotStatus.AVAILABLE, s.holdId = NULL, s.holdExpiresAt = NULL WHERE s.status = com.example.escaperoom.model.SlotStatus.HELD AND s.holdExpiresAt <= :now")
    int releaseExpired(@Param("now") Instant now);
}
