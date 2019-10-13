package io.freedriver.jsonlink;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ModeSerializer extends JsonSerializer<Mode> {
    @Override
    public void serialize(Mode mode, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeBoolean(mode.getModeValue());
    }
}
