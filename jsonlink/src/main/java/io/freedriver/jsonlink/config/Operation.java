package io.freedriver.jsonlink.config;

import java.util.Optional;

public enum Operation {
    ON,
    OFF,
    TOGGLE;

    public Optional<Boolean> fromDifferent(boolean currentState) {
        boolean nextState = from(currentState);
        if (nextState != currentState) {
            return Optional.of(nextState);
        }
        return Optional.empty();
    }

    public boolean from(boolean currentState) {
        switch (this) {
            case OFF:
                return false;
            case TOGGLE:
                return !currentState;
            default: // ON
                return true;
        }
    }
}
