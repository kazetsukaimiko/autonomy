package io.freedriver.autonomy.jpa.entity;

import java.time.Instant;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import io.freedriver.math.jpa.converter.measurement.CurrentConverter;
import io.freedriver.math.jpa.converter.measurement.EnergyConverter;
import io.freedriver.math.jpa.converter.measurement.PotentialConverter;
import io.freedriver.math.jpa.converter.measurement.PowerConverter;
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
import io.freedriver.victron.jpa.FirmwareVersionConverter;
import io.freedriver.victron.vedirect.OffReason;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Table(
        indexes = {
                @Index(columnList = "TIMESTAMP"),
                @Index(columnList = "SERIALNUMBER"),
                @Index(columnList = "OFFREASON"),
                @Index(columnList = "SERIALNUMBER,PRODUCTTYPE")
        }
)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class VEDirectMessage extends Event {

    @Enumerated(EnumType.STRING)
    private VictronProduct productType;

    @Enumerated(EnumType.STRING)
    private RelayState relayState;

    @Convert(converter = FirmwareVersionConverter.class)
    private FirmwareVersion firmwareVersion;

    private String serialNumber;

    @Convert(converter = PotentialConverter.class)
    private Potential mainVoltage;

    @Convert(converter = CurrentConverter.class)
    private Current mainCurrent;

    @Convert(converter = PotentialConverter.class)
    private Potential panelVoltage;

    @Convert(converter = PowerConverter.class)
    private Power panelPower;

    @Convert(converter = EnergyConverter.class)
    private Energy resettableYield;

    @Convert(converter = EnergyConverter.class)
    private Energy yieldToday;

    @Convert(converter = PowerConverter.class)
    private Power maxPowerToday;

    @Convert(converter = EnergyConverter.class)
    private Energy yieldYesterday;

    @Convert(converter = PowerConverter.class)
    private Power maxPowerYesterday;

    @Enumerated(EnumType.STRING)
    private StateOfOperation stateOfOperation;

    @Enumerated(EnumType.STRING)
    private TrackerOperation trackerOperation;

    @Enumerated(EnumType.STRING)
    private LoadOutputState loadOutputState;

    @Enumerated(EnumType.STRING)
    private ErrorCode errorCode;

    @Enumerated(EnumType.STRING)
    private OffReason offReason;

    public VEDirectMessage(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, VictronProduct productType, RelayState relayState, FirmwareVersion firmwareVersion, String serialNumber, Potential mainVoltage, Current mainCurrent, Potential panelVoltage, Power panelPower, Energy resettableYield, Energy yieldToday, Power maxPowerToday, Energy yieldYesterday, Power maxPowerYesterday, StateOfOperation stateOfOperation, TrackerOperation trackerOperation, LoadOutputState loadOutputState, ErrorCode errorCode, OffReason offReason) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.productType = productType;
        this.relayState = relayState;
        this.firmwareVersion = firmwareVersion;
        this.serialNumber = serialNumber;
        this.mainVoltage = mainVoltage;
        this.mainCurrent = mainCurrent;
        this.panelVoltage = panelVoltage;
        this.panelPower = panelPower;
        this.resettableYield = resettableYield;
        this.yieldToday = yieldToday;
        this.maxPowerToday = maxPowerToday;
        this.yieldYesterday = yieldYesterday;
        this.maxPowerYesterday = maxPowerYesterday;
        this.stateOfOperation = stateOfOperation;
        this.trackerOperation = trackerOperation;
        this.loadOutputState = loadOutputState;
        this.errorCode = errorCode;
        this.offReason = offReason;
    }

    public VEDirectMessage(io.freedriver.victron.VEDirectMessage veDirectMessage) {
        this(
                veDirectMessage.timestamp().toEpochMilli(),
                GenerationOrigin.NON_HUMAN,
                io.freedriver.victron.VEDirectMessage.class.getSimpleName(),
                veDirectMessage.serialNumber(),
                null,
                veDirectMessage.productType(),
                veDirectMessage.relayState(),
                veDirectMessage.firmwareVersion(),
                veDirectMessage.serialNumber(),
                veDirectMessage.mainVoltage(),
                veDirectMessage.mainCurrent(),
                veDirectMessage.panelVoltage(),
                veDirectMessage.panelPower(),
                veDirectMessage.resettableYield(),
                veDirectMessage.yieldToday(),
                veDirectMessage.maxPowerToday(),
                veDirectMessage.yieldYesterday(),
                veDirectMessage.maxPowerYesterday(),
                veDirectMessage.stateOfOperation(),
                veDirectMessage.trackerOperation(),
                veDirectMessage.loadOutputState(),
                veDirectMessage.errorCode(),
                veDirectMessage.offReason()
        );
    }

    @Transient
    public io.freedriver.victron.VEDirectMessage toNative() {
        return io.freedriver.victron.VEDirectMessage.builder()
                .timestamp(Instant.ofEpochMilli(getTimestamp()))
                .productType(getProductType())
                .relayState(getRelayState())
                .firmwareVersion(getFirmwareVersion())
                .serialNumber(getSerialNumber())
                .mainVoltage(getMainVoltage())
                .mainCurrent(getMainCurrent())
                .panelVoltage(getPanelVoltage())
                .panelPower(getPanelPower())
                .stateOfOperation(getStateOfOperation())
                .trackerOperation(getTrackerOperation())
                .loadOutputState(getLoadOutputState())
                .errorCode(getErrorCode())
                .offReason(getOffReason())
                .resettableYield(getResettableYield())
                .yieldToday(getYieldToday())
                .maxPowerToday(getMaxPowerToday())
                .yieldYesterday(getYieldYesterday())
                .maxPowerYesterday(getMaxPowerYesterday())
                .build();
    }

    public static int orderByTimestamp(VEDirectMessage veDirectMessage, VEDirectMessage veDirectMessage1) {
        if (veDirectMessage != null) {
            if (veDirectMessage1 != null) {
                return Long.compare(veDirectMessage.getTimestamp(), veDirectMessage1.getTimestamp());
            }
            return 1;
        }
        return veDirectMessage1 == null ? 0 : -1;
    }

}
