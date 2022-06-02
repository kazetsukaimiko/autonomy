package io.freedriver.autonomy.jpa.entity.event.sbms;

import io.freedriver.autonomy.jpa.entity.event.Event_;
import io.freedriver.math.measurement.types.electrical.Current;
import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.measurement.types.thermo.Temperature;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2022-06-01T15:59:12", comments="EclipseLink-2.7.6.v20200131-rNA")
@StaticMetamodel(SBMSMessage.class)
public class SBMSMessage_ extends Event_ {

    public static volatile SingularAttribute<SBMSMessage, Potential> cellFour;
    public static volatile SingularAttribute<SBMSMessage, Potential> cellOne;
    public static volatile SingularAttribute<SBMSMessage, Current> extCurrent;
    public static volatile SingularAttribute<SBMSMessage, Double> soc;
    public static volatile SingularAttribute<SBMSMessage, Potential> cellSix;
    public static volatile SingularAttribute<SBMSMessage, Temperature> externalTemperature;
    public static volatile SingularAttribute<SBMSMessage, Integer> errorCodes;
    public static volatile SingularAttribute<SBMSMessage, Boolean> charging;
    public static volatile SingularAttribute<SBMSMessage, Potential> cellTwo;
    public static volatile SingularAttribute<SBMSMessage, Current> pvCurrent1;
    public static volatile SingularAttribute<SBMSMessage, Current> pvCurrent2;
    public static volatile SingularAttribute<SBMSMessage, Potential> cellSeven;
    public static volatile SingularAttribute<SBMSMessage, Temperature> internalTemperature;
    public static volatile SingularAttribute<SBMSMessage, Boolean> discharging;
    public static volatile SingularAttribute<SBMSMessage, Potential> cellThree;
    public static volatile SingularAttribute<SBMSMessage, Potential> cellEight;
    public static volatile SingularAttribute<SBMSMessage, Potential> cellFive;
    public static volatile SingularAttribute<SBMSMessage, Current> batteryCurrent;

}