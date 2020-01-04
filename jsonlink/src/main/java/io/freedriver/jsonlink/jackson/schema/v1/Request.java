package io.freedriver.jsonlink.jackson.schema.v1;

import io.freedriver.jsonlink.Connector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class Request {
    private UUID uuid;
    private Map<Identifier, Mode> mode = new HashMap<>();
    private ReadRequest read;
    private WriteRequest write;
    private List<Identifier> turn_off = new ArrayList<>();
    private List<Identifier> turn_on = new ArrayList<>();

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

    public Request digitalRead(Identifier... pins) {
        return digitalRead(Stream.of(pins));
    }

    public Request digitalRead(Stream<Identifier> pins) {
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

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public Map<Identifier, Mode> getMode() {
        return mode;
    }

    public void setMode(Map<Identifier, Mode> mode) {
        this.mode = mode;
    }

    public List<Identifier> getTurn_off() {
        return turn_off;
    }

    public void setTurn_off(List<Identifier> turn_off) {
        this.turn_off = turn_off;
    }

    public List<Identifier> getTurn_on() {
        return turn_on;
    }

    public void setTurn_on(List<Identifier> turn_on) {
        this.turn_on = turn_on;
    }

    public Request turnOn(Stream<Identifier> pins) {
        pins.forEach(turn_on::add);
        return this;
    }

    public Request turnOff(Stream<Identifier> pins) {
        pins.forEach(turn_off::add);
        return this;
    }

    public Request newUuid() {
        setUuid(UUID.randomUUID());
        return this;
    }

    @Override
    public String toString() {
        return "Request{" +
                "uuid=" + uuid +
                ", mode=" + mode +
                ", read=" + read +
                ", write=" + write +
                '}';
    }

    public Response invoke(Connector connector) {
        return connector.send(this);
    }
}
