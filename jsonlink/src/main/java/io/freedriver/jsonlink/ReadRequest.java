package io.freedriver.jsonlink;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReadRequest {
    private Set<PinNumber> digital = new HashSet<>();
    private Map<PinNumber, Integer> analog = new HashMap<>();

    public Set<PinNumber> getDigital() {
        return digital;
    }

    public void setDigital(Set<PinNumber> digital) {
        this.digital = digital;
    }

    public Map<PinNumber, Integer> getAnalog() {
        return analog;
    }

    public void setAnalog(Map<PinNumber, Integer> analog) {
        this.analog = analog;
    }

    public ReadRequest readDigital(PinNumber pinNumber) {
        getDigital().add(pinNumber);
        return this;
    }

    public ReadRequest readAnalog(PinNumber pinNumber, Integer resistance) {
        getAnalog().put(pinNumber, resistance);
        return this;
    }

    public ReadRequest readAnalog(AnalogRead analogRead) {
        return readAnalog(analogRead.getPinNumber(), analogRead.getResistance());
    }
}
