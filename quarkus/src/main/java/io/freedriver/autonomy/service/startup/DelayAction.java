package io.freedriver.autonomy.service.startup;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.freedriver.jsonlink.config.v2.MappingsAction;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;

/**
 * Pauses one board's startup list. The limit is the annotations; the runner does not
 * check the range again. The runner waits, and it does not call {@link #accept}.
 */
@MappingsAction.Type("delay")
public final class DelayAction extends AutonomyMappingsAction {
    @NotNull
    @DurationMin(nanos = 1)
    @DurationMax(seconds = 10)
    private final Duration duration;

    /** ServiceLoader provider constructor. Not used to run the action. */
    public DelayAction() {
        this(null);
    }

    @JsonCreator
    public DelayAction(@JsonProperty("duration") Duration duration) {
        this.duration = duration;
    }

    public Duration getDuration() {
        return duration;
    }

    @Override
    public void accept(GpioControl gpio, ApplianceControl appliances) {
        throw new UnsupportedOperationException("the startup runner waits on a DelayAction");
    }
}
