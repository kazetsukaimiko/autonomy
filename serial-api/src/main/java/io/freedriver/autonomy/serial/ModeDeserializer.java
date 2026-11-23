package io.freedriver.autonomy.serial;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;

/**
 * Inverse of jsonlink 1.0.0 {@code ModeSerializer}, which writes {@link Mode#getModeValue()}.
 * {@code Mode.fromBoolean} in that jar returns the other constant, so a round trip uses
 * {@code getModeValue()} and not {@code fromBoolean}.
 */
final class ModeDeserializer extends JsonDeserializer<Mode> {

    @Override
    public Mode deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        boolean wire = parser.getBooleanValue();
        for (Mode mode : Mode.values()) {
            if (mode.getModeValue() == wire) {
                return mode;
            }
        }
        return (Mode) context.handleUnexpectedToken(Mode.class, parser);
    }
}
