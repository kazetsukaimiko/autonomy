package io.freedriver.autonomy.util;

import java.time.Instant;

public class MarkedTimeFrame<F> extends TimeFrame {
    private F fieldValue;

    public MarkedTimeFrame(Instant start, Instant finish, F fieldValue) {
        super(start, finish);
    }

    public F getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(F fieldValue) {
        this.fieldValue = fieldValue;
    }
}
