package de.otaku.birthdayevent.event;

import java.time.ZonedDateTime;

public final class EventStartDecision {
    private final BirthdayEventWindow window;

    public EventStartDecision(BirthdayEventWindow window) {
        this.window = window;
    }

    public boolean shouldTrigger(boolean alreadyTriggered, ZonedDateTime now) {
        return !alreadyTriggered && window.hasStarted(now) && now.isBefore(window.end());
    }
}
