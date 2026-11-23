package io.freedriver.autonomy.serial;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;

/**
 * The wire mapper both the serial gateway and the controller use for {@link SerialGatewayApi}.
 * Dates are ISO-8601. Unknown properties are ignored so jsonlink's {@code empty} flag on a write
 * reads back.
 */
public final class SerialGatewayJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .registerModule(module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private SerialGatewayJson() {
    }

    /**
     * Mode reader owned here until jsonlink gives {@link Mode} a {@code JsonCreator}.
     */
    public static Module module() {
        SimpleModule module = new SimpleModule("autonomy-serial-api");
        module.addDeserializer(Mode.class, new ModeDeserializer());
        return module;
    }

    /**
     * A copy of the shared mapper, so a caller can read and write without changing the shared one.
     */
    public static ObjectMapper mapper() {
        return MAPPER.copy();
    }
}
