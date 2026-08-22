package de.otaku.birthdayevent.event;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventStartDecisionTest {
    private final EventStartDecision decision = new EventStartDecision(new BirthdayEventWindow(EventSettings.defaults()));

    @Test
    void alreadyTriggeredEventDoesNotTriggerAgain() {
        assertFalse(decision.shouldTrigger(true, berlin(2026, 8, 22, 0, 10, 0)));
    }

    @Test
    void lateServerStartAfterMidnightTriggersOnceIfNotTriggered() {
        assertTrue(decision.shouldTrigger(false, berlin(2026, 8, 22, 0, 3, 0)));
    }

    @Test
    void runningOverMidnightTriggersGlobalStartExactlyOnce() {
        assertFalse(decision.shouldTrigger(false, berlin(2026, 8, 21, 23, 59, 59)));
        assertTrue(decision.shouldTrigger(false, berlin(2026, 8, 22, 0, 0, 0)));
        assertFalse(decision.shouldTrigger(true, berlin(2026, 8, 22, 0, 0, 1)));
    }

    @Test
    void noGlobalStartAfterEventEnd() {
        assertFalse(decision.shouldTrigger(false, berlin(2026, 8, 23, 0, 1, 0)));
    }

    private static ZonedDateTime berlin(int year, int month, int day, int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("Europe/Berlin"));
    }
}
