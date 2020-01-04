package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.jsonlink.jackson.schema.v1.AnalogRead;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

public class AnalogPin extends PinEntity {
    private float voltage;
    private float resistance;

    public AnalogPin() {
    }

    public AnalogPin(AnalogPin entity) {
        super(entity);
        this.voltage = entity.voltage;
        this.resistance = entity.resistance;
    }

    @Override
    Request apply(Request request) {
        return request.analogRead(new AnalogRead(getPin(), voltage, resistance));
    }

    public float getVoltage() {
        return voltage;
    }

    public void setVoltage(float voltage) {
        this.voltage = voltage;
    }

    public float getResistance() {
        return resistance;
    }

    public void setResistance(float resistance) {
        this.resistance = resistance;
    }
}
