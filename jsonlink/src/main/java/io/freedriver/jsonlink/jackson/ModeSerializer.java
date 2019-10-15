package io.freedriver.jsonlink.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;

import java.io.IOException;

public class ModeSerializer extends JsonSerializer<Mode> {
    @Override
    public void serialize(Mode mode, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeBoolean(mode.getModeValue());
    }
}
