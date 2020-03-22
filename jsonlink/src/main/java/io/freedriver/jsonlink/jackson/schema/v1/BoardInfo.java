package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.ArrayList;
import java.util.List;

public class BoardInfo {
    private List<Identifier> digitals = new ArrayList<>();
    private List<Identifier> analogs = new ArrayList<>();

    public BoardInfo() {
    }

    public List<Identifier> getDigitals() {
        return digitals;
    }

    public void setDigitals(List<Identifier> digitals) {
        this.digitals = digitals;
    }

    public List<Identifier> getAnalogs() {
        return analogs;
    }

    public void setAnalogs(List<Identifier> analogs) {
        this.analogs = analogs;
    }

    @Override
    public String toString() {
        return "BoardInfo{" +
                "digitals=" + digitals +
                ", analogs=" + analogs +
                '}';
    }
}
