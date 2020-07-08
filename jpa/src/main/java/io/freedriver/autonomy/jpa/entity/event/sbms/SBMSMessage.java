package io.freedriver.autonomy.jpa.entity.event.sbms;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import io.freedriver.electrodacus.sbms.ErrorCode;
import io.freedriver.math.jpa.converter.measurement.CurrentConverter;
import io.freedriver.math.jpa.converter.measurement.PotentialConverter;
import io.freedriver.math.jpa.converter.measurement.TemperatureConverter;
import io.freedriver.math.measurement.types.electrical.Current;
import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.measurement.types.thermo.Temperature;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.util.Objects;

@Table
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class SBMSMessage extends Event {

    private double soc;

    @Convert(converter = PotentialConverter.class)
    private Potential cellOne;
    @Convert(converter = PotentialConverter.class)
    private Potential cellTwo;
    @Convert(converter = PotentialConverter.class)
    private Potential cellThree;
    @Convert(converter = PotentialConverter.class)
    private Potential cellFour;
    @Convert(converter = PotentialConverter.class)
    private Potential cellFive;
    @Convert(converter = PotentialConverter.class)
    private Potential cellSix;
    @Convert(converter = PotentialConverter.class)
    private Potential cellSeven;
    @Convert(converter = PotentialConverter.class)
    private Potential cellEight;

    @Convert(converter = TemperatureConverter.class)
    private Temperature internalTemperature;
    @Convert(converter = TemperatureConverter.class)
    private Temperature externalTemperature;

    private boolean charging;
    private boolean discharging;

    @Convert(converter = CurrentConverter.class)
    private Current batteryCurrent;
    @Convert(converter = CurrentConverter.class)
    private Current pvCurrent1;
    @Convert(converter = CurrentConverter.class)
    private Current pvCurrent2;
    @Convert(converter = CurrentConverter.class)
    private Current extCurrent;

    private int errorCodes;


    public SBMSMessage(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, double soc, Potential cellOne, Potential cellTwo, Potential cellThree, Potential cellFour, Potential cellFive, Potential cellSix, Potential cellSeven, Potential cellEight, Temperature internalTemperature, Temperature externalTemperature, boolean charging, boolean discharging, Current batteryCurrent, Current pvCurrent1, Current pvCurrent2, Current extCurrent, int errorCodes) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.soc = soc;
        this.cellOne = cellOne;
        this.cellTwo = cellTwo;
        this.cellThree = cellThree;
        this.cellFour = cellFour;
        this.cellFive = cellFive;
        this.cellSix = cellSix;
        this.cellSeven = cellSeven;
        this.cellEight = cellEight;
        this.internalTemperature = internalTemperature;
        this.externalTemperature = externalTemperature;
        this.charging = charging;
        this.discharging = discharging;
        this.batteryCurrent = batteryCurrent;
        this.pvCurrent1 = pvCurrent1;
        this.pvCurrent2 = pvCurrent2;
        this.extCurrent = extCurrent;
        this.errorCodes = errorCodes;
    }

    public SBMSMessage(io.freedriver.electrodacus.sbms.SBMSMessage message) {
        this(
                message.getTimestamp().toEpochMilli(),
                GenerationOrigin.NON_HUMAN,
                io.freedriver.electrodacus.sbms.SBMSMessage.class.getSimpleName(),
                message.getPath().toString(),
                null,
                message.getSoc(),
                message.getCellOne(),
                message.getCellTwo(),
                message.getCellThree(),
                message.getCellFour(),
                message.getCellFive(),
                message.getCellSix(),
                message.getCellSeven(),
                message.getCellEight(),
                message.getInternalTemperature(),
                message.getExternalTemperature(),
                message.isCharging(),
                message.isDischarging(),
                message. getBatteryCurrent(),
                message.getPvCurrent1(),
                message.getPvCurrent2(),
                message.getExtCurrent(),
                message.getErrorCodes().stream().collect(ErrorCode.encoding())
        );
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
