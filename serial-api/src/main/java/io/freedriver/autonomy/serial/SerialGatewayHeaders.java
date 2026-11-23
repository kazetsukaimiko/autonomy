package io.freedriver.autonomy.serial;

/**
 * Header names on {@link SerialGatewayApi}.
 */
public final class SerialGatewayHeaders {

    /**
     * Write credential. A read passes null. A write sends the token file contents.
     * The gateway compares the header to that file.
     */
    public static final String WRITE_TOKEN = "X-Autonomy-Serial-Write";

    private SerialGatewayHeaders() {
    }
}
