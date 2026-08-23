package io.freedriver.autonomy.jpa.entity;

import java.util.UUID;
import java.util.function.Function;

import io.freedriver.autonomy.event.GenerationOrigin;

/**
 * Maps native Victron VE.Direct messages onto {@link VEDirectMessage} and back.
 */
public final class VEDirectMessageConverter
        implements Function<io.freedriver.victron.VEDirectMessage, VEDirectMessage> {
    public static final VEDirectMessageConverter INSTANCE = new VEDirectMessageConverter();

    @Override
    public VEDirectMessage apply(io.freedriver.victron.VEDirectMessage veDirectMessage) {
        return new VEDirectMessage(
                UUID.randomUUID().toString(),
                veDirectMessage.timestamp(),
                GenerationOrigin.NON_HUMAN,
                io.freedriver.victron.VEDirectMessage.class.getSimpleName(),
                veDirectMessage.serialNumber(),
                null,
                veDirectMessage.productType(),
                veDirectMessage.relayState(),
                veDirectMessage.firmwareVersion(),
                veDirectMessage.serialNumber(),
                veDirectMessage.mainVoltage(),
                veDirectMessage.mainCurrent(),
                veDirectMessage.panelVoltage(),
                veDirectMessage.panelPower(),
                veDirectMessage.resettableYield(),
                veDirectMessage.yieldToday(),
                veDirectMessage.maxPowerToday(),
                veDirectMessage.yieldYesterday(),
                veDirectMessage.maxPowerYesterday(),
                veDirectMessage.stateOfOperation(),
                veDirectMessage.trackerOperation(),
                veDirectMessage.loadOutputState(),
                veDirectMessage.errorCode(),
                veDirectMessage.offReason());
    }

    public io.freedriver.victron.VEDirectMessage toNative(VEDirectMessage message) {
        return io.freedriver.victron.VEDirectMessage.builder()
                .timestamp(message.timestamp())
                .productType(message.productType())
                .relayState(message.relayState())
                .firmwareVersion(message.firmwareVersion())
                .serialNumber(message.serialNumber())
                .mainVoltage(message.mainVoltage())
                .mainCurrent(message.mainCurrent())
                .panelVoltage(message.panelVoltage())
                .panelPower(message.panelPower())
                .stateOfOperation(message.stateOfOperation())
                .trackerOperation(message.trackerOperation())
                .loadOutputState(message.loadOutputState())
                .errorCode(message.errorCode())
                .offReason(message.offReason())
                .resettableYield(message.resettableYield())
                .yieldToday(message.yieldToday())
                .maxPowerToday(message.maxPowerToday())
                .yieldYesterday(message.yieldYesterday())
                .maxPowerYesterday(message.maxPowerYesterday())
                .build();
    }
}
