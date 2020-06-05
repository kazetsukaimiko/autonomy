package io.freedriver.autonomy.jpa.entity.event;

/**
 * The origin of the event.
 */
public enum SourceType {
    HUMAN(true),
    HUMAN_ORIGIN(true),
    NON_HUMAN(false),
    NON_HUMAN_ORIGIN(false);

    private final boolean humanOriginEvent;

    SourceType(boolean humanOriginEvent) {
        this.humanOriginEvent = humanOriginEvent;
    }

    public boolean isHumanOriginEvent() {
        return humanOriginEvent;
    }
}
