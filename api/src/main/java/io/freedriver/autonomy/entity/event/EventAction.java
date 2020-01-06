package io.freedriver.autonomy.entity.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.freedriver.autonomy.util.Base64Serialization;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

@JsonSerialize(using = EventAction.Serializer.class)
@JsonDeserialize(using = EventAction.Deserializer.class)
@FunctionalInterface
public interface EventAction extends Serializable {
    Optional<Event> fire(Event sourceEvent);

    class Serializer extends JsonSerializer<EventAction> {
        @Override
        public void serialize(EventAction eventAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            Optional<String> encoded = Base64Serialization.encode(eventAction);
            if (encoded.isPresent()) {
                jsonGenerator.writeString(encoded.get());
            } else {
                jsonGenerator.writeNull();
            }
        }
    }
    class Deserializer extends JsonDeserializer<EventAction> {
        @Override
        public EventAction deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return Base64Serialization.decode(jsonParser.getValueAsString(), EventAction.class)
                    .orElse(null);
        }
    }
}
