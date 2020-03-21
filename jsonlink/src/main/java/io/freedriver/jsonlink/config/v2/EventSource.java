package io.freedriver.jsonlink.config.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventSource {
    private String source;
    private List<Reaction> reactions = new ArrayList<>();

    public EventSource() {
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Reaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<Reaction> reactions) {
        this.reactions = reactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventSource that = (EventSource) o;
        return Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    @Override
    public String toString() {
        return "EventSource{" +
                "source='" + source + '\'' +
                ", reactions=" + reactions +
                '}';
    }
}
