package io.freedriver.jsonlink;

import java.util.HashMap;
import java.util.Map;

public class ModeRequest {
    private Map<PinNumber, Mode> modes = new HashMap<>();

    public Map<PinNumber, Mode> getModes() {
        return modes;
    }

    public void setModes(Map<PinNumber, Mode> modes) {
        this.modes = modes;
    }

    public ModeRequest setMode(ModeSet pinMode) {
        getModes().put(pinMode.getPinNumber(), pinMode.getMode());
        return this;
    }
}
