package io.freedriver.autonomy.jpa.entity.event;

public enum EventPriority {
    LOW(FailurePolicy.NO_CARES_IN_THE_WORLD),
    STANDARD(FailurePolicy.COMPLAIN_AND_SHRUG),
    HIGH(FailurePolicy.INITIATE_SOUL_SEARCHING),
    CRITICAL(FailurePolicy.PANIC_FIRST_ASK_QUESTIONS_LATER);

    private final FailurePolicy policy;

    EventPriority(FailurePolicy policy) {
        this.policy = policy;
    }

    public FailurePolicy getPolicy() {
        return policy;
    }

}
