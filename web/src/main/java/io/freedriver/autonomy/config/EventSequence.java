package io.freedriver.autonomy.config;

import io.freedriver.autonomy.entity.JoystickEvent;

public class EventSequence {
    private JoystickEvent.Type start;
    private JoystickEvent.Type end;
    private EventDuration duration;
    private String group;

    public EventSequence() {
    }

    public EventSequence(JoystickEvent.Type start, JoystickEvent.Type end, EventDuration duration, String group) {
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.group = group;
    }

    public JoystickEvent.Type getStart() {
        return start;
    }

    public void setStart(JoystickEvent.Type start) {
        this.start = start;
    }

    public JoystickEvent.Type getEnd() {
        return end;
    }

    public void setEnd(JoystickEvent.Type end) {
        this.end = end;
    }

    public EventDuration getDuration() {
        return duration;
    }

    public void setDuration(EventDuration duration) {
        this.duration = duration;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
