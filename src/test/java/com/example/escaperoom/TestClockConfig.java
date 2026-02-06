package com.example.escaperoom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class TestClockConfig {

    public static class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        private final ZoneId zone = ZoneId.of("UTC");

        public void setNow(Instant instant) {
            now.set(instant);
        }

        public void advanceSeconds(long seconds) {
            now.updateAndGet(i -> i.plusSeconds(seconds));
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }

    @Bean
    @Primary
    public MutableClock clock() {
        return new MutableClock();
    }
}
