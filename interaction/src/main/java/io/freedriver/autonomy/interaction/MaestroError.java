package io.freedriver.autonomy.interaction;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.freedriver.autonomy.interaction.ErrorType.SCRIPT;
import static io.freedriver.autonomy.interaction.ErrorType.SERIAL;

public enum MaestroError {

    SIGNAL_ERROR            (SERIAL, 0, (byte) 0b00000001),
    OVERRUN_ERROR           (SERIAL, 0, (byte) 0b00000010),
    BUFFER_FULL             (SERIAL, 0, (byte) 0b00000100),
    CRC_ERROR               (SERIAL, 0, (byte) 0b00001000),
    PROTOCOL_ERROR          (SERIAL, 0, (byte) 0b00010000),
    TIMEOUT                 (SERIAL, 0, (byte) 0b00100000),
    STACK_ERROR             (SCRIPT, 0, (byte) 0b01000000),
    CALL_STACK_ERROR        (SCRIPT, 0, (byte) 0b10000000),
    PROGRAM_COUNTER_ERROR   (SCRIPT, 1, (byte) 0b00000001),
    ;


    private final ErrorType errorType;
    private final int byteIndex;
    private final byte errorBit;

    MaestroError(ErrorType errorType, int byteIndex, byte errorBit) {
        this.errorType = errorType;
        this.byteIndex = byteIndex;
        this.errorBit = errorBit;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public int getByteIndex() {
        return byteIndex;
    }

    public byte getErrorBit() {
        return errorBit;
    }

    public static Set<MaestroError> readErrors(byte[] bytes) {
        return Stream.of(MaestroError.values())
                .filter(maestroError -> maestroError.matches(bytes))
                .collect(Collectors.toSet());
    }

    public boolean matches(byte[] bytes) {
        return matches(bytes[getByteIndex()]);
    }

    private boolean matches(byte singleByte) {
        return (singleByte & getErrorBit()) != 0;
    }

    @Override
    public String toString() {
        return "MaestroError{" +
                "type=" + getErrorType().name() +
                "name=" + name() +
                "errorBit=" + errorBit +
                '}';
    }
}
