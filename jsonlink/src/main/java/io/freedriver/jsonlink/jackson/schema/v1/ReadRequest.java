package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReadRequest {
    private Set<Identifier> digital = new HashSet<>();
    private Map<Identifier, Integer> analog = new HashMap<>();

    public Set<Identifier> getDigital() {
        return digital;
    }

    public void setDigital(Set<Identifier> digital) {
        this.digital = digital;
    }

    public Map<Identifier, Integer> getAnalog() {
        return analog;
    }

    public void setAnalog(Map<Identifier, Integer> analog) {
        this.analog = analog;
    }

    public ReadRequest readDigital(Identifier pinNumber) {
        getDigital().add(pinNumber);
        return this;
    }

    public ReadRequest readAnalog(Identifier pinNumber, Integer resistance) {
        getAnalog().put(pinNumber, resistance);
        return this;
    }

    public ReadRequest readAnalog(AnalogRead analogRead) {
        return readAnalog(analogRead.getPinNumber(), analogRead.getResistance());
    }
}
