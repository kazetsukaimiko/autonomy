package io.freedriver.jsonlink.jackson;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

import java.io.IOException;

public class PinNumberKeyDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
        return Identifier.of(Integer.parseInt(s));
    }
}
