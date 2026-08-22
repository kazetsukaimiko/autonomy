package io.freedriver.autonomy.jpa.entity;

import java.time.Instant;

import io.freedriver.autonomy.event.Event;
import io.freedriver.autonomy.event.GenerationOrigin;
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

public record VEDirectMessage(
        String id,
        Instant timestamp,
        GenerationOrigin generationOrigin,
        String sourceClass,
        String sourceId,
        String eventId,
        VictronProduct productType,
        RelayState relayState,
        FirmwareVersion firmwareVersion,
        String serialNumber,
        Potential mainVoltage,
        Current mainCurrent,
        Potential panelVoltage,
        Power panelPower,
        Energy resettableYield,
        Energy yieldToday,
        Power maxPowerToday,
        Energy yieldYesterday,
        Power maxPowerYesterday,
        StateOfOperation stateOfOperation,
        TrackerOperation trackerOperation,
        LoadOutputState loadOutputState,
        ErrorCode errorCode,
        OffReason offReason)
        implements Event {}
