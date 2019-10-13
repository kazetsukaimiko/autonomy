package io.freedriver.jsonlink;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class JsonLinkModule extends SimpleModule {
    public JsonLinkModule() {
        addKeyDeserializer(PinNumber.class, new PinNumberKeyDeserializer());
        addSerializer(Mode.class, new ModeSerializer());
    }
}
