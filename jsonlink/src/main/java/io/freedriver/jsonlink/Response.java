package io.freedriver.jsonlink;

import java.util.Map;

public class Response {
    private Map<PinNumber, Boolean> digital;
    private Map<PinNumber, Integer> analog;

    public Map<PinNumber, Boolean> getDigital() {
        return digital;
    }

    public void setDigital(Map<PinNumber, Boolean> digital) {
        this.digital = digital;
    }

    public Map<PinNumber, Integer> getAnalog() {
        return analog;
    }

    public void setAnalog(Map<PinNumber, Integer> analog) {
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
