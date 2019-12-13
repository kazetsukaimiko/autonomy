package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.Map;
import java.util.UUID;

public class Response {
    private UUID uuid;
    private Map<Identifier, Boolean> digital;
    private Map<Identifier, Integer> analog;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Map<Identifier, Boolean> getDigital() {
        return digital;
    }

    public void setDigital(Map<Identifier, Boolean> digital) {
        this.digital = digital;
    }

    public Map<Identifier, Integer> getAnalog() {
        return analog;
    }

    public void setAnalog(Map<Identifier, Integer> analog) {
        this.analog = analog;
    }

    @Override
    public String toString() {
        return "Response{" +
                "digital=" + digital +
                ", analog=" + analog +
                '}';
    }
}
