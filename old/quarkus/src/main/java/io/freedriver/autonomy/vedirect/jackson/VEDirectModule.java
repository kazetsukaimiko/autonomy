package io.freedriver.autonomy.vedirect.jackson;


import com.fasterxml.jackson.databind.module.SimpleModule;
import io.freedriver.math.measurement.types.Measurement;
import io.freedriver.math.measurement.types.electrical.*;
import io.freedriver.math.number.ScaledNumber;

import java.util.function.Function;

public class VEDirectModule extends SimpleModule {
    public VEDirectModule() {
        mBidirectional(Potential.class, Potential::new);
        mBidirectional(Current.class, Current::new);
        mBidirectional(Power.class, Power::new);
        mBidirectional(Charge.class, Charge::new);
        mBidirectional(Resistance.class, Resistance::new);
        mBidirectional(Energy.class, Energy::new);
    }

    public <M extends Measurement<M>> void mBidirectional(Class<M> klazz, Function<ScaledNumber, M> constructor) {
        mSerializer(klazz);
        mDeserializer(klazz, constructor);
    }

    public <M extends Measurement<M>> void mSerializer(Class<M> klazz) {
        addSerializer(klazz, new MeasurementSerializer<>());
    }

    public <M extends Measurement<M>> void mDeserializer(Class<M> klazz, Function<ScaledNumber, M> constructor) {
        addDeserializer(klazz, new MeasurementDeserializer<>(constructor));
    }
}
