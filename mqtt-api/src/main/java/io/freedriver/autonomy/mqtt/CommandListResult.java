package io.freedriver.autonomy.mqtt;

/**
 * Outcome of {@link CommandListSink#accept}. {@link Applied} names the list
 * that was stored. {@link Refused} is a list the controller rejected whole.
 * {@link Dropped} is a list the controller ignored.
 */
public sealed interface CommandListResult
        permits CommandListResult.Applied, CommandListResult.Refused, CommandListResult.Dropped {

    /**
     * @param listId the contract list id, canonical lowercase UUID text
     */
    record Applied(String listId) implements CommandListResult {

        public Applied {
            if (listId == null || listId.isBlank()) {
                throw new IllegalArgumentException("listId");
            }
        }
    }

    /**
     * @param reason why the list was rejected whole
     */
    record Refused(String reason) implements CommandListResult {

        public Refused {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("reason");
            }
        }
    }

    /**
     * @param reason why the list was ignored
     */
    record Dropped(String reason) implements CommandListResult {

        public Dropped {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("reason");
            }
        }
    }
}
