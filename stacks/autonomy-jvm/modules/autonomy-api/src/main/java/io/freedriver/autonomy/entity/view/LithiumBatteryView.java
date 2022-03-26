package io.freedriver.autonomy.entity.view;

import io.freedriver.autonomy.jpa.entity.event.sbms.SBMSMessage;
import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.electrical.Current;
import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.measurement.types.electrical.Power;
import io.freedriver.math.number.ScaledNumber;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class LithiumBatteryView {
    private List<String> sourceIds = new ArrayList<>();
    private Map<Integer, Double> cells = new LinkedHashMap<>();
    private Current current = new Current(ScaledNumber.ZERO);
    private Power power = new Power(ScaledNumber.ZERO);

    public LithiumBatteryView(SBMSMessage... sbmsMessage) {
        this(Stream.of(sbmsMessage));
    }

    public LithiumBatteryView(Stream<SBMSMessage> sbmsMessageStream) {
        int[] cellNum = new int[] {0};
        sbmsMessageStream.forEach(sbmsMessage -> {
            sourceIds.add(sbmsMessage.getSourceId());
            this.current = current.add(sbmsMessage.getBatteryCurrent());
            this.power = power.add(sbmsMessage.totalPower());
            cells.put(1+cellNum[0], sbmsMessage.getCellOne().doubleValue());
            cells.put(2+cellNum[0], sbmsMessage.getCellTwo().doubleValue());
            cells.put(3+cellNum[0], sbmsMessage.getCellThree().doubleValue());
            cells.put(4+cellNum[0], sbmsMessage.getCellFour().doubleValue());
            cells.put(5+cellNum[0], sbmsMessage.getCellFive().doubleValue());
            cells.put(6+cellNum[0], sbmsMessage.getCellSix().doubleValue());
            cells.put(7+cellNum[0], sbmsMessage.getCellSeven().doubleValue());
            cells.put(8+cellNum[0], sbmsMessage.getCellEight().doubleValue());
            cellNum[0] = cellNum[0] + 8;
        });
    }

    public List<String> getSourceIds() {
        return sourceIds;
    }

    public void setSourceIds(List<String> sourceIds) {
        this.sourceIds = sourceIds;
    }

    public Map<Integer, Double> getCells() {
        return cells;
    }

    public void setCells(Map<Integer, Double> cells) {
        this.cells = cells;
    }

    public Potential getVoltage() {
        return new Potential(new ScaledNumber(cells
                .values()
                .stream()
                .mapToDouble(d -> d)
                .sum(), UnitPrefix.ONE));
    }

    public Power getPower() {
        return getVoltage().toPower(getCurrent());
    }

    public Current getCurrent() {
        return current;
    }

    public void setCurrent(Current current) {
        this.current = current;
    }
}
