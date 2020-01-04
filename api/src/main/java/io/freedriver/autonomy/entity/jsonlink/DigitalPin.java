package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

public class DigitalPin extends PinEntity {
    public DigitalPin() {
    }

    public DigitalPin(DigitalPin entity) {
        super(entity);
    }

    @Override
    Request apply(Request request) {
        return (getMode() == Mode.INPUT) ?
                request.digitalRead(getPin())
                :
                request;
    }
}
