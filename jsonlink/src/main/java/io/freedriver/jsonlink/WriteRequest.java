package io.freedriver.jsonlink;

import java.util.HashMap;
import java.util.Map;

public class WriteRequest {
    private Map<PinNumber, Boolean> digital = new HashMap<>();

    public Map<PinNumber, Boolean> getDigital() {
        return digital;
    }

    public void setDigital(Map<PinNumber, Boolean> digital) {
        this.digital = digital;
    }

    public WriteRequest writeDigital(DigitalWrite pinWrite) {
        getDigital().put(pinWrite.getPinNumber(), pinWrite.isOperation());
        return this;
    }
}
