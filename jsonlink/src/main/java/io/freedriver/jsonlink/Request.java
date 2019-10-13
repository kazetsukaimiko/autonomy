package io.freedriver.jsonlink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Request {
    private Map<PinNumber, Mode> mode = new HashMap<>();
    private ReadRequest read;
    private WriteRequest write;
    private List<PinNumber> turn_off = new ArrayList<>();
    private List<PinNumber> turn_on = new ArrayList<>();

    public Request analogRead(AnalogRead... analogReads) {
        return analogRead(Stream.of(analogReads));
    }

    public Request analogRead(Stream<AnalogRead> analogReads) {
        if (read == null) {
            read = new ReadRequest();
        }
        analogReads.forEach(read::readAnalog);
        return this;
    }

    public Request modeSet(ModeSet... modes) {
        return modeSet(Stream.of(modes));
    }

    public Request modeSet(Stream<ModeSet> modes) {
        modes.forEach(this::addMode);
        return this;
    }

    private void addMode(ModeSet modeSet) {
        getMode().put(modeSet.getPinNumber(), modeSet.getMode());
    }

    public Request digitalRead(PinNumber... pins) {
        return digitalRead(Stream.of(pins));
    }

    public Request digitalRead(Stream<PinNumber> pins) {
        if (read == null) {
            read = new ReadRequest();
        }
        pins.forEach(read.getDigital()::add);
        return this;
    }

    public Request digitalWrite(DigitalWrite... pinWrites) {
        return digitalWrite(Stream.of(pinWrites));
    }

    public Request digitalWrite(Stream<DigitalWrite> pinWrite) {
        if (write == null) {
            write = new WriteRequest();
        }
        pinWrite.forEach(write::writeDigital);
        return this;
    }

    public ReadRequest getRead() {
        return read;
    }

    public void setRead(ReadRequest read) {
        this.read = read;
    }

    public WriteRequest getWrite() {
        return write;
    }

    public void setWrite(WriteRequest write) {
        this.write = write;
    }

    public Map<PinNumber, Mode> getMode() {
        return mode;
    }

    public void setMode(Map<PinNumber, Mode> mode) {
        this.mode = mode;
    }

    public List<PinNumber> getTurn_off() {
        return turn_off;
    }

    public void setTurn_off(List<PinNumber> turn_off) {
        this.turn_off = turn_off;
    }

    public List<PinNumber> getTurn_on() {
        return turn_on;
    }

    public void setTurn_on(List<PinNumber> turn_on) {
        this.turn_on = turn_on;
    }

    public Request turnOn(Stream<PinNumber> pins) {
        pins.forEach(turn_on::add);
        return this;
    }

    public Request turnOff(Stream<PinNumber> pins) {
        pins.forEach(turn_off::add);
        return this;
    }

}
