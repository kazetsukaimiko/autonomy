package io.freedriver.autonomy.vedirect.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import kaze.math.measurement.types.Measurement;
import kaze.math.number.ScaledNumber;

import java.io.IOException;
import java.util.function.Function;

public class MeasurementDeserializer<M extends Measurement<M>> extends JsonDeserializer<M> {
    private final Function<ScaledNumber, M> constructor;

    protected MeasurementDeserializer(Function<ScaledNumber, M> constructor) {
        this.constructor = constructor;
    }

    @Override
    public M deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        return constructor.apply(ScaledNumber.of(jsonParser.getDecimalValue()));
    }
}
