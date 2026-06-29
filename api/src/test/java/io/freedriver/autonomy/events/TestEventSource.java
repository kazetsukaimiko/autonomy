package io.freedriver.autonomy.events;

public final class TestEventSource implements EventSource {
    @Override
    public String name() {
        return "test";
    }

    @Override
    public void start() {
        // test stub
    }

    @Override
    public void stop() {
        // test stub
    }
}