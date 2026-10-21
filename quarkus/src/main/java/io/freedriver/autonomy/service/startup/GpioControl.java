package io.freedriver.autonomy.service.startup;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

/** Sets one output to a stated value. Implementations must not toggle. */
public interface GpioControl {
    void setPin(Identifier pin, boolean on);
}
