package io.freedriver.jsonlink;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PinNumberSerializer extends JsonSerializer<PinNumber> {

    @Override
    public void serialize(PinNumber pinNumber, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(pinNumber.getPin());
    }
}
