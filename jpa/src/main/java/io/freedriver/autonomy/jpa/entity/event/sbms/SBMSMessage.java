package io.freedriver.autonomy.jpa.entity.event.sbms;

import io.freedriver.autonomy.jpa.entity.event.*;
import kaze.math.measurement.types.electrical.Current;
import kaze.math.measurement.types.electrical.Potential;
import kaze.math.measurement.types.thermo.Temperature;
import kaze.serial.ErrorCode;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

@Table
@Entity
public class SBMSMessage extends Event {

    private double soc;
    private Potential cellOne;
    private Potential cellTwo;
    private Potential cellThree;
    private Potential cellFour;
    private Potential cellFive;
    private Potential cellSix;
    private Potential cellSeven;
    private Potential cellEight;

    private Temperature internalTemperature;
    private Temperature externalTemperature;

    private boolean charging;
    private boolean discharging;

    private Current batteryCurrent;
    private Current pvCurrent1;
    private Current pvCurrent2;
    private Current extCurrent;
    private int errorCodes;

    public SBMSMessage(kaze.serial.SBMSMessage message) {
        super(
                message.getTimestamp().toEpochMilli(),
                new EventCoordinate(message.getPath().toString(), "SBMS0"),
                new EventDescription(StateType.MAINTAIN_STATE, "REPORT"),
                SourceType.NON_HUMAN,
                EventPriority.STANDARD);

        this.soc = message.getSoc();
        this.cellOne = message.getCellOne();
        this.cellTwo = message.getCellTwo();
        this.cellThree = message.getCellThree();
        this.cellFour = message.getCellFour();
        this.cellFive = message.getCellFive();
        this.cellSix = message.getCellSix();
        this.cellSeven = message.getCellSeven();
        this.cellEight = message.getCellEight();

        this.internalTemperature = message.getInternalTemperature();
        this.externalTemperature = message.getExternalTemperature();

        this.charging = message.isCharging();
        this.discharging = message.isDischarging();

        this.batteryCurrent = message.getBatteryCurrent();
        this.pvCurrent1 = message.getPvCurrent1();
        this.pvCurrent2 = message.getPvCurrent2();
        this.extCurrent = message.getExtCurrent();
        this.errorCodes = message.getErrorCodes().stream()
            .collect(ErrorCode.encoding());
    }

    public SBMSMessage() {
    }

    public double getSoc() {
        return soc;
    }

    public void setSoc(double soc) {
        this.soc = soc;
    }

    public Potential getCellOne() {
        return cellOne;
    }

    public void setCellOne(Potential cellOne) {
        this.cellOne = cellOne;
    }

    public Potential getCellTwo() {
        return cellTwo;
    }

    public void setCellTwo(Potential cellTwo) {
        this.cellTwo = cellTwo;
    }

    public Potential getCellThree() {
        return cellThree;
    }

    public void setCellThree(Potential cellThree) {
        this.cellThree = cellThree;
    }

    public Potential getCellFour() {
        return cellFour;
    }

    public void setCellFour(Potential cellFour) {
        this.cellFour = cellFour;
    }

    public Potential getCellFive() {
        return cellFive;
    }

    public void setCellFive(Potential cellFive) {
        this.cellFive = cellFive;
    }

    public Potential getCellSix() {
        return cellSix;
    }

    public void setCellSix(Potential cellSix) {
        this.cellSix = cellSix;
    }

    public Potential getCellSeven() {
        return cellSeven;
    }

    public void setCellSeven(Potential cellSeven) {
        this.cellSeven = cellSeven;
    }

    public Potential getCellEight() {
        return cellEight;
    }

    public void setCellEight(Potential cellEight) {
        this.cellEight = cellEight;
    }

    public Temperature getInternalTemperature() {
        return internalTemperature;
    }

    public void setInternalTemperature(Temperature internalTemperature) {
        this.internalTemperature = internalTemperature;
    }

    public Temperature getExternalTemperature() {
        return externalTemperature;
    }

    public void setExternalTemperature(Temperature externalTemperature) {
        this.externalTemperature = externalTemperature;
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public boolean isDischarging() {
        return discharging;
    }

    public void setDischarging(boolean discharging) {
        this.discharging = discharging;
    }

    public Current getBatteryCurrent() {
        return batteryCurrent;
    }

    public void setBatteryCurrent(Current batteryCurrent) {
        this.batteryCurrent = batteryCurrent;
    }

    public Current getPvCurrent1() {
        return pvCurrent1;
    }

    public void setPvCurrent1(Current pvCurrent1) {
        this.pvCurrent1 = pvCurrent1;
    }

    public Current getPvCurrent2() {
        return pvCurrent2;
    }

    public void setPvCurrent2(Current pvCurrent2) {
        this.pvCurrent2 = pvCurrent2;
    }

    public Current getExtCurrent() {
        return extCurrent;
    }

    public void setExtCurrent(Current extCurrent) {
        this.extCurrent = extCurrent;
    }

    public int getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(int errorCodes) {
        this.errorCodes = errorCodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SBMSMessage that = (SBMSMessage) o;
        return Double.compare(that.soc, soc) == 0 &&
                charging == that.charging &&
                discharging == that.discharging &&
                Objects.equals(cellOne, that.cellOne) &&
                Objects.equals(cellTwo, that.cellTwo) &&
                Objects.equals(cellThree, that.cellThree) &&
                Objects.equals(cellFour, that.cellFour) &&
                Objects.equals(cellFive, that.cellFive) &&
                Objects.equals(cellSix, that.cellSix) &&
                Objects.equals(cellSeven, that.cellSeven) &&
                Objects.equals(cellEight, that.cellEight) &&
                Objects.equals(internalTemperature, that.internalTemperature) &&
                Objects.equals(externalTemperature, that.externalTemperature) &&
                Objects.equals(batteryCurrent, that.batteryCurrent) &&
                Objects.equals(pvCurrent1, that.pvCurrent1) &&
                Objects.equals(pvCurrent2, that.pvCurrent2) &&
                Objects.equals(extCurrent, that.extCurrent) &&
                Objects.equals(errorCodes, that.errorCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), soc, cellOne, cellTwo, cellThree, cellFour, cellFive, cellSix, cellSeven, cellEight, internalTemperature, externalTemperature, charging, discharging, batteryCurrent, pvCurrent1, pvCurrent2, extCurrent, errorCodes);
    }
}
