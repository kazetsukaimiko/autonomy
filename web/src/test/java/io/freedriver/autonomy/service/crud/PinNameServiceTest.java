package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.PinNameEntity;
import io.freedriver.jsonlink.config.PinName;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import org.dizitart.no2.Nitrite;

import java.util.function.Function;

public class PinNameServiceTest extends NitriteCRUDServiceUnitTest<PinNameEntity, PinNameService> {
    @Override
    Class<PinNameEntity> getKlazz() {
        return PinNameEntity.class;
    }

    @Override
    Function<Nitrite, PinNameService> getConstructor() {
        return PinNameService::new;
    }

    @Override
    PinNameEntity create(int idx) {
        PinNameEntity pinName = new PinNameEntity();
        pinName.setPinNumber(Identifier.of(idx));
        pinName.setPinName(randomName());
        pinName.setPinMode(Mode.OUTPUT);
        return pinName;
    }

}
