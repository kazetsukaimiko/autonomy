package io.freedriver.autonomy.async;

import io.freedriver.victron.VEDirectMessage;
import io.freedriver.victron.VictronProduct;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public enum VEDirectHistoricalOperations {
    VOLTAGE_CHANGE(VEDirectHistoricalOperations::handleVoltageTrends);

    private final BiFunction<VictronProduct, List<VEDirectMessage>, Optional<String>> messageGenerator;

    VEDirectHistoricalOperations(BiFunction<VictronProduct, List<VEDirectMessage>, Optional<String>> messageGenerator) {
        this.messageGenerator = messageGenerator;
    }

    // Find the beginning of the day.
    /*
    public static Instant beginningOfDay(List<VEDirectMessage> history, OffReason example) {
        Optional<MarkedTimeFrame<OffReason>> thisMorning = TimeFrame.blocks(history.stream(), VEDirectMessage::getTimestamp, VEDirectMessage::getOffReason)
                .stream()
                // Between 12AM and 12PM
                .filter(block -> block.getStart().isAfter(LocalDate.now().atStartOfDay(UTC).toInstant()))
                .filter(block -> block.getStart().isBefore(LocalDate.now().atStartOfDay(UTC).plus(Duration.of(12, HOURS)).toInstant()))
                // Cause is no panel power
                .filter(block -> block.getFieldValue() == OffReason.NO_INPUT_POWER)
                // TimeFrame is greater than two hours.
                .filter(block -> block.greaterThan(Duration.of(2, HOURS)))
                // Get the last instance.
                .max(Comparator.comparing(TimeFrame::getStart));


        Optional<Instant> lastTimeOff = history.stream()
                .filter(message -> message.getOffReason() == OffReason.NO_INPUT_POWER)
                .max(Comparator.comparing(VEDirectMessage::getTimestamp))
                .map(VEDirectMessage::getTimestamp);



    }
*/


    public static Optional<String> handleVoltageTrends(VictronProduct product, List<VEDirectMessage> history) {
        /*
        if (history.size() == 1) {
            return Optional.of
        }
        history.stream()
                .filter()

         */
        return Optional.empty();
    }
}
