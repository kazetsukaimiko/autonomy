package io.freedriver.autonomy.service;

import java.util.Map;

/**
 * Appliance on/off values read from the state file.
 * {@link Status#MISSING} and {@link Status#UNREADABLE} both mean nothing is restored.
 */
record LoadedApplianceState(Status status, Map<String, Boolean> states) {

    enum Status {
        MISSING,
        UNREADABLE,
        PRESENT
    }

    LoadedApplianceState {
        states = states == null ? Map.of() : Map.copyOf(states);
    }

    static LoadedApplianceState missing() {
        return new LoadedApplianceState(Status.MISSING, Map.of());
    }

    static LoadedApplianceState unreadable() {
        return new LoadedApplianceState(Status.UNREADABLE, Map.of());
    }

    static LoadedApplianceState present(Map<String, Boolean> states) {
        return new LoadedApplianceState(Status.PRESENT, states);
    }
}
