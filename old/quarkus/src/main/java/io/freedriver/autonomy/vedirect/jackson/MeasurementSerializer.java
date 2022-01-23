package io.freedriver.autonomy.vedirect.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.freedriver.math.measurement.types.Measurement;

import java.io.IOException;

public class MeasurementSerializer<M extends Measurement<M>> extends JsonSerializer<M> {
    @Override
    public void serialize(M m, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(m.getValue().descale());
    }
}
