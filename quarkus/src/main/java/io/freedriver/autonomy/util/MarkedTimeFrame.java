package io.freedriver.autonomy.util;

import java.time.Instant;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class MarkedTimeFrame<F> extends TimeFrame {
    private F fieldValue;

    public MarkedTimeFrame(Instant start, Instant finish, F fieldValue) {
        super(start, finish);
        this.fieldValue = fieldValue;
    }
}
