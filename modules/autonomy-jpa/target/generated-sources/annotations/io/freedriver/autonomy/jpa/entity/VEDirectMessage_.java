package io.freedriver.autonomy.jpa.entity;

import io.freedriver.autonomy.jpa.entity.event.Event_;
import io.freedriver.math.measurement.types.electrical.Current;
import io.freedriver.math.measurement.types.electrical.Energy;
import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.measurement.types.electrical.Power;
import io.freedriver.victron.ErrorCode;
import io.freedriver.victron.FirmwareVersion;
import io.freedriver.victron.LoadOutputState;
import io.freedriver.victron.RelayState;
import io.freedriver.victron.StateOfOperation;
import io.freedriver.victron.TrackerOperation;
import io.freedriver.victron.VictronProduct;
import io.freedriver.victron.vedirect.OffReason;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2022-06-01T15:59:12", comments="EclipseLink-2.7.6.v20200131-rNA")
@StaticMetamodel(VEDirectMessage.class)
public class VEDirectMessage_ extends Event_ {

    public static volatile SingularAttribute<VEDirectMessage, String> serialNumber;
    public static volatile SingularAttribute<VEDirectMessage, Energy> yieldToday;
    public static volatile SingularAttribute<VEDirectMessage, LoadOutputState> loadOutputState;
    public static volatile SingularAttribute<VEDirectMessage, ErrorCode> errorCode;
    public static volatile SingularAttribute<VEDirectMessage, Power> maxPowerToday;
    public static volatile SingularAttribute<VEDirectMessage, Current> mainCurrent;
    public static volatile SingularAttribute<VEDirectMessage, StateOfOperation> stateOfOperation;
    public static volatile SingularAttribute<VEDirectMessage, Power> panelPower;
    public static volatile SingularAttribute<VEDirectMessage, Potential> panelVoltage;
    public static volatile SingularAttribute<VEDirectMessage, Energy> resettableYield;
    public static volatile SingularAttribute<VEDirectMessage, Energy> yieldYesterday;
    public static volatile SingularAttribute<VEDirectMessage, RelayState> relayState;
    public static volatile SingularAttribute<VEDirectMessage, TrackerOperation> trackerOperation;
    public static volatile SingularAttribute<VEDirectMessage, Potential> mainVoltage;
    public static volatile SingularAttribute<VEDirectMessage, Power> maxPowerYesterday;
    public static volatile SingularAttribute<VEDirectMessage, OffReason> offReason;
    public static volatile SingularAttribute<VEDirectMessage, FirmwareVersion> firmwareVersion;
    public static volatile SingularAttribute<VEDirectMessage, VictronProduct> productType;

}