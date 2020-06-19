package io.freedriver.autonomy.jpa.entity;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import io.freedriver.math.measurement.types.electrical.Current;
import io.freedriver.math.measurement.types.electrical.Energy;
import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.measurement.types.electrical.Power;
import io.freedriver.victron.*;
import io.freedriver.victron.vedirect.OffReason;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

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
public class VEDirectMessage extends Event {

    @Enumerated(EnumType.STRING)
    private VictronProduct productType;

    @Enumerated(EnumType.STRING)
    private RelayState relayState;

    private FirmwareVersion firmwareVersion;

    private String serialNumber;

    private Potential mainVoltage;

    private Current mainCurrent;

    private Potential panelVoltage;

    private Power panelPower;

    private Energy resettableYield;

    private Energy yieldToday;

    private Power maxPowerToday;

    private Energy yieldYesterday;

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

    public VEDirectMessage() {
    }

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
                veDirectMessage.getTimestamp().toEpochMilli(),
                GenerationOrigin.NON_HUMAN,
                io.freedriver.victron.VEDirectMessage.class.getSimpleName(),
                veDirectMessage.getSerialNumber(),
                null,
                veDirectMessage.getProductType(),
                veDirectMessage.getRelayState(),
                veDirectMessage.getFirmwareVersion(),
                veDirectMessage.getSerialNumber(),
                veDirectMessage.getMainVoltage(),
                veDirectMessage.getMainCurrent(),
                veDirectMessage.getPanelVoltage(),
                veDirectMessage.getPanelPower(),
                veDirectMessage.getResettableYield(),
                veDirectMessage.getYieldToday(),
                veDirectMessage.getMaxPowerToday(),
                veDirectMessage.getYieldYesterday(),
                veDirectMessage.getMaxPowerYesterday(),
                veDirectMessage.getStateOfOperation(),
                veDirectMessage.getTrackerOperation(),
                veDirectMessage.getLoadOutputState(),
                veDirectMessage.getErrorCode(),
                veDirectMessage.getOffReason()
        );
    }

    public VictronProduct getProductType() {
        return productType;
    }

    public void setProductType(VictronProduct productType) {
        this.productType = productType;
    }

    public RelayState getRelayState() {
        return relayState;
    }

    public void setRelayState(RelayState relayState) {
        this.relayState = relayState;
    }

    public FirmwareVersion getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(FirmwareVersion firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Potential getMainVoltage() {
        return mainVoltage;
    }

    public void setMainVoltage(Potential mainVoltage) {
        this.mainVoltage = mainVoltage;
    }

    public Current getMainCurrent() {
        return mainCurrent;
    }

    public void setMainCurrent(Current mainCurrent) {
        this.mainCurrent = mainCurrent;
    }

    public Potential getPanelVoltage() {
        return panelVoltage;
    }

    public void setPanelVoltage(Potential panelVoltage) {
        this.panelVoltage = panelVoltage;
    }

    public Power getPanelPower() {
        return panelPower;
    }

    public void setPanelPower(Power panelPower) {
        this.panelPower = panelPower;
    }


    public Energy getResettableYield() {
        return resettableYield;
    }

    public void setResettableYield(Energy resettableYield) {
        this.resettableYield = resettableYield;
    }

    public Energy getYieldToday() {
        return yieldToday;
    }

    public void setYieldToday(Energy yieldToday) {
        this.yieldToday = yieldToday;
    }

    public Power getMaxPowerToday() {
        return maxPowerToday;
    }

    public void setMaxPowerToday(Power maxPowerToday) {
        this.maxPowerToday = maxPowerToday;
    }

    public Energy getYieldYesterday() {
        return yieldYesterday;
    }

    public void setYieldYesterday(Energy yieldYesterday) {
        this.yieldYesterday = yieldYesterday;
    }

    public Power getMaxPowerYesterday() {
        return maxPowerYesterday;
    }

    public void setMaxPowerYesterday(Power maxPowerYesterday) {
        this.maxPowerYesterday = maxPowerYesterday;
    }

    public StateOfOperation getStateOfOperation() {
        return stateOfOperation;
    }

    public void setStateOfOperation(StateOfOperation stateOfOperation) {
        this.stateOfOperation = stateOfOperation;
    }

    public TrackerOperation getTrackerOperation() {
        return trackerOperation;
    }

    public void setTrackerOperation(TrackerOperation trackerOperation) {
        this.trackerOperation = trackerOperation;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public LoadOutputState getLoadOutputState() {
        return loadOutputState;
    }

    public void setLoadOutputState(LoadOutputState loadOutputState) {
        this.loadOutputState = loadOutputState;
    }

    public OffReason getOffReason() {
        return offReason;
    }

    public void setOffReason(OffReason offReason) {
        this.offReason = offReason;
    }

    @Transient
    public io.freedriver.victron.VEDirectMessage toNative() {
        io.freedriver.victron.VEDirectMessage nativeMessage = new io.freedriver.victron.VEDirectMessage();
        nativeMessage.setTimestamp(Instant.ofEpochMilli(getTimestamp()));
        nativeMessage.setProductType(getProductType());
        nativeMessage.setRelayState(getRelayState());
        nativeMessage.setFirmwareVersion(getFirmwareVersion());
        nativeMessage.setSerialNumber(getSerialNumber());
        nativeMessage.setMainVoltage(getMainVoltage());
        nativeMessage.setMainCurrent(getMainCurrent());
        nativeMessage.setPanelVoltage(getPanelVoltage());
        nativeMessage.setPanelPower(getPanelPower());
        nativeMessage.setStateOfOperation(getStateOfOperation());
        nativeMessage.setTrackerOperation(getTrackerOperation());
        nativeMessage.setLoadOutputState(getLoadOutputState());
        nativeMessage.setErrorCode(getErrorCode());
        nativeMessage.setOffReason(getOffReason());
        nativeMessage.setResettableYield(getResettableYield());
        nativeMessage.setYieldToday(getYieldToday());
        nativeMessage.setMaxPowerToday(getMaxPowerToday());
        nativeMessage.setYieldYesterday(getYieldYesterday());
        nativeMessage.setMaxPowerYesterday(getMaxPowerYesterday());
        return nativeMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        VEDirectMessage that = (VEDirectMessage) o;
        return  productType == that.productType &&
                relayState == that.relayState &&
                Objects.equals(firmwareVersion, that.firmwareVersion) &&
                Objects.equals(serialNumber, that.serialNumber) &&
                Objects.equals(mainVoltage, that.mainVoltage) &&
                Objects.equals(mainCurrent, that.mainCurrent) &&
                Objects.equals(panelVoltage, that.panelVoltage) &&
                Objects.equals(panelPower, that.panelPower) &&
                Objects.equals(resettableYield, that.resettableYield) &&
                Objects.equals(yieldToday, that.yieldToday) &&
                Objects.equals(maxPowerToday, that.maxPowerToday) &&
                Objects.equals(yieldYesterday, that.yieldYesterday) &&
                Objects.equals(maxPowerYesterday, that.maxPowerYesterday) &&
                stateOfOperation == that.stateOfOperation &&
                trackerOperation == that.trackerOperation &&
                loadOutputState == that.loadOutputState &&
                errorCode == that.errorCode &&
                offReason == that.offReason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), productType, relayState, firmwareVersion, serialNumber, mainVoltage, mainCurrent, panelVoltage, panelPower, resettableYield, yieldToday, maxPowerToday, yieldYesterday, maxPowerYesterday, stateOfOperation, trackerOperation, loadOutputState, errorCode, offReason);
    }

    @Override
    public String toString() {
        return "VEDirectMessage{" +
                ", productType=" + productType +
                ", relayState=" + relayState +
                ", firmwareVersion=" + firmwareVersion +
                ", serialNumber='" + serialNumber + '\'' +
                ", stateOfOperation=" + stateOfOperation +
                ", trackerOperation=" + trackerOperation +
                ", loadOutputState=" + loadOutputState +
                ", errorCode=" + errorCode +
                ", offReason=" + offReason +
                '}';
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
