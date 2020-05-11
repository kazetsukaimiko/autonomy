package io.freedriver.autonomy.jpa.converter;

import kaze.victron.FirmwareVersion;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Optional;

@Converter(autoApply = true)
public class FirmwareVersionConverter implements AttributeConverter<FirmwareVersion, String> {
    @Override
    public String convertToDatabaseColumn(FirmwareVersion firmwareVersion) {
        return Optional.ofNullable(firmwareVersion)
                .map(FirmwareVersion::toString)
                .orElse(null);
    }

    @Override
    public FirmwareVersion convertToEntityAttribute(String s) {
        return Optional.ofNullable(s)
                .map(FirmwareVersion::new)
                .orElse(null);
    }
}
