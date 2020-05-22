package io.freedriver.autonomy.jpa.converter;

import kaze.victron.FirmwareVersion;
import org.junit.jupiter.api.Test;

public class FirmwareVersionConverterTest {

    FirmwareVersionConverter victim = new FirmwareVersionConverter();

    @Test
    public void testVOneFiveO() {
        FirmwareVersion firmwareVersion = victim.convertToEntityAttribute("v1.50");
    }
}
