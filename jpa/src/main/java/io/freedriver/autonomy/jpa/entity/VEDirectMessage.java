package io.freedriver.autonomy.jpa.entity;

import kaze.math.measurement.units.Current;
import kaze.math.measurement.units.Energy;
import kaze.math.measurement.units.Potential;
import kaze.math.measurement.units.Power;
import kaze.victron.ErrorCode;
import kaze.victron.FirmwareVersion;
import kaze.victron.LoadOutputState;
import kaze.victron.RelayState;
import kaze.victron.StateOfOperation;
import kaze.victron.TrackerOperation;
import kaze.victron.VictronProduct;
import kaze.victron.vedirect.OffReason;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Table(
        indexes = {
                @Index(columnList = "TIMESTAMP"),
                @Index(columnList = "SERIALNUMBER"),
                @Index(columnList = "OFFREASON"),
        }
)
@Entity
public class VEDirectMessage {
    @Id
    @GeneratedValue
    private Long id;

    private long timestamp = Instant.now().toEpochMilli();

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

    public VEDirectMessage(kaze.victron.VEDirectMessage veDirectMessage) {
        this.timestamp = veDirectMessage.getTimestamp().toEpochMilli();
        this.productType = veDirectMessage.getProductType();
        this.relayState = veDirectMessage.getRelayState();
        this.firmwareVersion = veDirectMessage.getFirmwareVersion();
        this.serialNumber = veDirectMessage.getSerialNumber();
        this.mainVoltage = veDirectMessage.getMainVoltage();

        this.mainCurrent = veDirectMessage.getMainCurrent();
        this.panelVoltage = veDirectMessage.getPanelVoltage();
        this.panelPower = veDirectMessage.getPanelPower();
        this.resettableYield = veDirectMessage.getResettableYield();
        this.yieldToday = veDirectMessage.getYieldToday();
        this.maxPowerToday = veDirectMessage.getMaxPowerToday();
        this.yieldYesterday = veDirectMessage.getYieldYesterday();
        this.maxPowerYesterday = veDirectMessage.getMaxPowerYesterday();

        this.stateOfOperation = veDirectMessage.getStateOfOperation();
        this.trackerOperation = veDirectMessage.getTrackerOperation();
        this.loadOutputState = veDirectMessage.getLoadOutputState();
        this.errorCode = veDirectMessage.getErrorCode();
        this.offReason = veDirectMessage.getOffReason();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VEDirectMessage that = (VEDirectMessage) o;
        return timestamp == that.timestamp &&
                Objects.equals(id, that.id) &&
                productType == that.productType &&
                relayState == that.relayState &&
                Objects.equals(firmwareVersion, that.firmwareVersion) &&
                Objects.equals(serialNumber, that.serialNumber) &&
                stateOfOperation == that.stateOfOperation &&
                trackerOperation == that.trackerOperation &&
                loadOutputState == that.loadOutputState &&
                errorCode == that.errorCode &&
                offReason == that.offReason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, productType, relayState, firmwareVersion, serialNumber, stateOfOperation, trackerOperation, loadOutputState, errorCode, offReason);
    }

    @Override
    public String toString() {
        return "VEDirectMessage{" +
                "id=" + id +
                ", timestamp=" + timestamp +
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

    /*
    @Override
    public String toString() {
        return "VEDirectMessage{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", productType=" + productType +
                ", relayState=" + relayState +
                ", firmwareVersion=" + firmwareVersion +
                ", serialNumber='" + serialNumber + '\'' +
                ", mainVoltage=" + mainVoltage +
                ", mainCurrent=" + mainCurrent +
                ", panelVoltage=" + panelVoltage +
                ", panelPower=" + panelPower +
                ", stateOfOperation=" + stateOfOperation +
                ", trackerOperation=" + trackerOperation +
                ", loadOutputState=" + loadOutputState +
                ", errorCode=" + errorCode +
                ", offReason=" + offReason +
                ", resettableYield=" + resettableYield +
                ", yieldToday=" + yieldToday +
                ", maxPowerToday=" + maxPowerToday +
                ", yieldYesterday=" + yieldYesterday +
                ", maxPowerYesterday=" + maxPowerYesterday +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VEDirectMessage that = (VEDirectMessage) o;
        return id == that.id &&
                Objects.equals(timestamp, that.timestamp) &&
                productType == that.productType &&
                relayState == that.relayState &&
                Objects.equals(firmwareVersion, that.firmwareVersion) &&
                Objects.equals(serialNumber, that.serialNumber) &&
                Objects.equals(mainVoltage, that.mainVoltage) &&
                Objects.equals(mainCurrent, that.mainCurrent) &&
                Objects.equals(panelVoltage, that.panelVoltage) &&
                Objects.equals(panelPower, that.panelPower) &&
                stateOfOperation == that.stateOfOperation &&
                trackerOperation == that.trackerOperation &&
                loadOutputState == that.loadOutputState &&
                errorCode == that.errorCode &&
                offReason == that.offReason &&
                Objects.equals(resettableYield, that.resettableYield) &&
                Objects.equals(yieldToday, that.yieldToday) &&
                Objects.equals(maxPowerToday, that.maxPowerToday) &&
                Objects.equals(yieldYesterday, that.yieldYesterday) &&
                Objects.equals(maxPowerYesterday, that.maxPowerYesterday);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, productType, relayState, firmwareVersion, serialNumber, mainVoltage, mainCurrent, panelVoltage, panelPower, stateOfOperation, trackerOperation, loadOutputState, errorCode, offReason, resettableYield, yieldToday, maxPowerToday, yieldYesterday, maxPowerYesterday);
    }*/
}
