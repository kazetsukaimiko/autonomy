package io.freedriver.autonomy.entity.event;

import static io.freedriver.autonomy.entity.event.FailurePolicy.*;

public enum EventPriority {
    LOW(NO_CARES_IN_THE_WORLD),
    STANDARD(COMPLAIN_AND_SHRUG),
    HIGH(INITIATE_SOUL_SEARCHING),
    CRITICAL(PANIC_FIRST_ASK_QUESTIONS_LATER);

    private final FailurePolicy policy;

    EventPriority(FailurePolicy policy) {
        this.policy = policy;
    }

    public FailurePolicy getPolicy() {
        return policy;
    }

}
