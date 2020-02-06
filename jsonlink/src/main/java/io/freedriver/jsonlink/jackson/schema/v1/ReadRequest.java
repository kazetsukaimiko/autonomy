package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class ReadRequest {
    private Set<Identifier> digital = new HashSet<>();
    private Set<AnalogRead> analog = new LinkedHashSet<>();

    public Set<Identifier> getDigital() {
        return digital;
    }

    public void setDigital(Set<Identifier> digital) {
        this.digital = digital;
    }

    public Set<AnalogRead> getAnalog() {
        return analog;
    }

    public void setAnalog(Set<AnalogRead> analog) {
        this.analog = analog;
    }

    public ReadRequest readDigital(Identifier pinNumber) {
        getDigital().add(pinNumber);
        return this;
    }

    public ReadRequest readAnalog(Identifier pinNumber, float voltage, float resistance) {
        getAnalog().add(new AnalogRead(pinNumber, voltage, resistance));
        return this;
    }

    public ReadRequest readAnalog(AnalogRead analogRead) {
        return readAnalog(
                analogRead.getPin(),
                analogRead.getVoltage(),
                analogRead.getResistance());
    }

    public boolean isEmpty() {
        return digital.isEmpty() && analog.isEmpty();
    }

    @Override
    public String toString() {
        return "ReadRequest{" +
                "digital=" + digital +
                ", analog=" + analog +
                '}';
    }
}
