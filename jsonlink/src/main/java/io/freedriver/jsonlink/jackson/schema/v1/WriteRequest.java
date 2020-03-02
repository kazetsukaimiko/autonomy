package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.HashMap;
import java.util.Map;

public class WriteRequest {
    private Map<Identifier, DigitalState> digital = new HashMap<>();

    public Map<Identifier, DigitalState> getDigital() {
        return digital;
    }

    public void setDigital(Map<Identifier, DigitalState> digital) {
        this.digital = digital;
    }

    public WriteRequest writeDigital(DigitalWrite pinWrite) {
        getDigital().put(pinWrite.getPinNumber(), pinWrite.getOperation());
        return this;
    }

    public boolean isEmpty() {
        return digital.isEmpty();
    }
}
