package io.freedriver.autonomy.jpa.converter;

import io.freedriver.victron.FirmwareVersion;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Optional;
import java.util.logging.Logger;

@Converter(autoApply = true)
public class FirmwareVersionConverter implements AttributeConverter<FirmwareVersion, String> {
    private static final Logger LOGGER = Logger.getLogger(FirmwareVersionConverter.class.getName());

    @Override
    public String convertToDatabaseColumn(FirmwareVersion firmwareVersion) {
        return Optional.ofNullable(firmwareVersion)
                .map(FirmwareVersion::toString)
                .orElse(null);
    }

    @Override
    public FirmwareVersion convertToEntityAttribute(String s) {
        if (s != null) {
            try {
                return new FirmwareVersion(s);
            } catch (IllegalArgumentException iae) {
                // TODO LOGGER.info("")
                return null;
            }
        }
        return null;
    }
}
