package io.freedriver.autonomy.config;


import io.freedriver.autonomy.entity.event.input.joystick.JoystickEvent;

public class EventSequence {
    private JoystickEvent start;
    private JoystickEvent end;
    private EventDuration duration;
    private String group;

    public EventSequence() {
    }

    public EventSequence(JoystickEvent start, JoystickEvent end, EventDuration duration, String group) {
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.group = group;
    }

    public JoystickEvent getStart() {
        return start;
    }

    public void setStart(JoystickEvent start) {
        this.start = start;
    }

    public JoystickEvent getEnd() {
        return end;
    }

    public void setEnd(JoystickEvent end) {
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
