package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

public class DigitalPin extends PinEntity {
    public DigitalPin() {
    }

    public DigitalPin(DigitalPin entity) {
        super(entity);
    }

    public DigitalPin(Identifier pin, String name, Mode mode) {
        super(pin, name, mode);
    }

    @Override
    Request apply(Request request) {
        return (getMode() == Mode.INPUT) ?
                request.digitalRead(getPin())
                :
                request;
    }
}
