package io.freedriver.autonomy.serial;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;

/**
 * Jackson module for the serial gateway wire. ServiceLoader finds it
 * ({@code META-INF/services/com.fasterxml.jackson.databind.Module}); a REST mapper calls
 * {@link ObjectMapper#findAndRegisterModules()}.
 *
 * <p>jsonlink {@code Response.created} is an {@link java.time.Instant}, so this module writes dates
 * as ISO-8601. It also ignores unknown properties so a write's {@code empty} flag reads back.
 */
public final class SerialGatewayModule extends SimpleModule {

    public SerialGatewayModule() {
        super("autonomy-serial-api");
        addDeserializer(Mode.class, new ModeDeserializer());
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        if (context.getOwner() instanceof ObjectMapper mapper) {
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
    }
}
