package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.HashMap;
import java.util.Map;

public class WriteRequest {
    private Map<Identifier, Boolean> digital = new HashMap<>();

    public Map<Identifier, Boolean> getDigital() {
        return digital;
    }

    public void setDigital(Map<Identifier, Boolean> digital) {
        this.digital = digital;
    }

    public WriteRequest writeDigital(DigitalWrite pinWrite) {
        getDigital().put(pinWrite.getPinNumber(), pinWrite.isOperation());
        return this;
    }

    public boolean isEmpty() {
        return digital.isEmpty();
    }
}
