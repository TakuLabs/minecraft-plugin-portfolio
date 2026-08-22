package de.otaku.birthdayevent.event;

import java.time.ZonedDateTime;

public enum EventSchedulerMode {
    DELAYED_START,
    SAFETY_ONLY,
    NONE;

    public static EventSchedulerMode forState(BirthdayEventWindow window, boolean alreadyTriggered, ZonedDateTime now) {
        if (now.isBefore(window.start())) {
            return alreadyTriggered ? NONE : DELAYED_START;
        }
        if (now.isBefore(window.end())) {
            return alreadyTriggered ? NONE : SAFETY_ONLY;
        }
        return NONE;
    }
}
