package io.freedriver.jsonlink.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

public class JsonLinkModule extends SimpleModule {
    public JsonLinkModule() {
        addKeyDeserializer(Identifier.class, new PinNumberKeyDeserializer());
        addSerializer(Mode.class, new ModeSerializer());
    }
}
