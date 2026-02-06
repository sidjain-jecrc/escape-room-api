package com.example.escaperoom;

import com.example.escaperoom.api.dto.HoldResponse;
import com.example.escaperoom.model.Slot;
import com.example.escaperoom.model.SlotStatus;
import com.example.escaperoom.service.ApiExceptions;
import com.example.escaperoom.service.SlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestClockConfig.class)
class SlotServiceTest {

    @Autowired SlotService service;
    @Autowired TestClockConfig.MutableClock clock;

    @Test
    void holdExpiresAndCannotBeConfirmed() {
        HoldResponse hold = service.holdSlot(1L);
        UUID holdId = hold.holdId();

        // Advance beyond the configured 5 seconds
        clock.advanceSeconds(6);

        ApiExceptions.Gone ex = assertThrows(ApiExceptions.Gone.class, () -> service.confirmHold(holdId));
        assertTrue(ex.getMessage().toLowerCase().contains("expired"));

        Slot slot = service.getSlot(1L);
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
    }

    @Test
    void concurrentHoldOnlyOneWins() throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<HoldResponse> task = () -> {
            start.await(2, TimeUnit.SECONDS);
            return service.holdSlot(2L);
        };

        Future<HoldResponse> f1 = exec.submit(task);
        Future<HoldResponse> f2 = exec.submit(task);

        start.countDown();

        HoldResponse r1 = f1.get(2, TimeUnit.SECONDS);
        ExecutionException maybeFail = null;
        try {
            f2.get(2, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            maybeFail = e;
        }

        assertNotNull(r1.holdId());
        assertNotNull(maybeFail, "Second hold should fail");
        assertTrue(maybeFail.getCause() instanceof ApiExceptions.Conflict);

        exec.shutdownNow();
    }

    @Test
    void confirmVsExpireRaceNoDoubleBooking() throws Exception {
        // Create a hold that expires at t+5s
        HoldResponse hold = service.holdSlot(3L);
        UUID holdId = hold.holdId();

        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Boolean> confirmer = exec.submit(() -> {
            start.await(2, TimeUnit.SECONDS);
            try {
                service.confirmHold(holdId);
                return true; // booked
            } catch (ApiExceptions.Gone ignored) {
                return false; // expired
            }
        });

        Future<Void> expirer = exec.submit(() -> {
            start.await(2, TimeUnit.SECONDS);
            // jump to exactly expiration boundary
            clock.setNow(Instant.parse("2026-01-01T00:00:05Z"));
            // Trigger cleanup via any service call (we use holdSlot on a missing id to avoid affecting existing slots)
            try {
                service.holdSlot(9999L);
            } catch (ApiExceptions.NotFound ignored) {}
            return null;
        });

        start.countDown();
        boolean booked = confirmer.get(2, TimeUnit.SECONDS);
        expirer.get(2, TimeUnit.SECONDS);

        Slot slot = service.getSlot(3L);
        assertTrue(slot.getStatus() == SlotStatus.BOOKED || slot.getStatus() == SlotStatus.AVAILABLE);

        // If booked, must be via the same holdId and should never revert.
        if (booked) {
            assertEquals(SlotStatus.BOOKED, slot.getStatus());
        }

        exec.shutdownNow();
    }
}
