package io.freedriver.autonomy.jpa.converter.event;

import io.freedriver.autonomy.jpa.entity.event.EventAction;
import io.freedriver.autonomy.jpa.util.Base64Serialization;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EventActionConverter implements AttributeConverter<EventAction, String> {
    @Override
    public String convertToDatabaseColumn(EventAction eventAction) {
        return Base64Serialization.encode(eventAction).orElse(null);
    }

    @Override
    public EventAction convertToEntityAttribute(String s) {
        return Base64Serialization.decode(s, EventAction.class).orElse(null);
    }
}
