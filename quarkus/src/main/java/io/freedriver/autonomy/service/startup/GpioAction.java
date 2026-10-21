package io.freedriver.autonomy.service.startup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.freedriver.jsonlink.config.v2.MappingsAction;
import jakarta.validation.constraints.NotNull;

/** Sets one appliance's pin to {@code on}. Replaying it leaves that same value. */
@MappingsAction.Type("gpio")
public final class GpioAction extends AutonomyMappingsAction {
    @NotNull
    private final String appliance;

    @NotNull
    private final Boolean on;

    /** ServiceLoader provider constructor. Not used to run the action. */
    public GpioAction() {
        this(null, null);
    }

    @JsonCreator
    public GpioAction(@JsonProperty("appliance") String appliance, @JsonProperty("on") Boolean on) {
        this.appliance = appliance;
        this.on = on;
    }

    public String getAppliance() {
        return appliance;
    }

    public Boolean getOn() {
        return on;
    }

    @Override
    public void accept(GpioControl gpio, ApplianceControl appliances) {
        if (appliance == null || on == null) {
            throw new IllegalArgumentException("gpio action is missing appliance or on");
        }
        gpio.setPin(appliances.pin(appliance), on);
    }
}
