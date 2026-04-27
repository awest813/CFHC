package simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured summary of one season-flow advancement. Existing UI bridges are
 * still called by {@link SeasonController}; this result gives headless hosts a
 * stable signal they can inspect without scraping UI callbacks.
 */
public final class SeasonAdvanceResult {
    public enum EventType {
        WEEK_ADVANCED,
        STATUS_UPDATED,
        NEEDS_DIALOG,
        NOTIFICATION,
        RECRUITING_STARTED,
        REFRESH_REQUESTED
    }

    public enum DialogType {
        AWARDS_SUMMARY,
        MIDSEASON_SUMMARY,
        SEASON_SUMMARY,
        CONTRACT,
        JOB_OFFERS,
        PROMOTIONS,
        REDSHIRT_LIST,
        TRANSFER_LIST,
        REALIGNMENT_SUMMARY
    }

    public static final class Event {
        public final EventType type;
        public final DialogType dialogType;
        public final String statusText;
        public final String buttonText;
        public final boolean majorEvent;
        public final String title;
        public final String message;

        private Event(EventType type, DialogType dialogType, String statusText, String buttonText,
                      boolean majorEvent, String title, String message) {
            this.type = type;
            this.dialogType = dialogType;
            this.statusText = statusText;
            this.buttonText = buttonText;
            this.majorEvent = majorEvent;
            this.title = title;
            this.message = message;
        }
    }

    public static final class Builder {
        private final int weekBefore;
        private int weekAfter;
        private final List<Event> events = new ArrayList<>();

        Builder(int weekBefore) {
            this.weekBefore = weekBefore;
            this.weekAfter = weekBefore;
        }

        public Builder weekAfter(int weekAfter) {
            this.weekAfter = weekAfter;
            return this;
        }

        public Builder weekAdvanced() {
            events.add(new Event(EventType.WEEK_ADVANCED, null, null, null, false, null, null));
            return this;
        }

        public Builder statusUpdated(String statusText, String buttonText, boolean majorEvent) {
            events.add(new Event(EventType.STATUS_UPDATED, null, statusText, buttonText, majorEvent, null, null));
            return this;
        }

        public Builder needsDialog(DialogType dialogType, String message) {
            events.add(new Event(EventType.NEEDS_DIALOG, dialogType, null, null, false, null, message));
            return this;
        }

        public Builder notification(String title, String message) {
            events.add(new Event(EventType.NOTIFICATION, null, null, null, false, title, message));
            return this;
        }

        public Builder recruitingStarted() {
            events.add(new Event(EventType.RECRUITING_STARTED, null, null, null, false, null, null));
            return this;
        }

        public Builder refreshRequested() {
            events.add(new Event(EventType.REFRESH_REQUESTED, null, null, null, false, null, null));
            return this;
        }

        public SeasonAdvanceResult build() {
            return new SeasonAdvanceResult(weekBefore, weekAfter, events);
        }
    }

    public final int weekBefore;
    public final int weekAfter;
    public final List<Event> events;

    private SeasonAdvanceResult(int weekBefore, int weekAfter, List<Event> events) {
        this.weekBefore = weekBefore;
        this.weekAfter = weekAfter;
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
    }

    public boolean hasEvent(EventType type) {
        for (Event event : events) {
            if (event.type == type) {
                return true;
            }
        }
        return false;
    }

    public Event lastStatusEvent() {
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            if (event.type == EventType.STATUS_UPDATED) {
                return event;
            }
        }
        return null;
    }
}
