package io.freedriver.autonomy.serial.api;

/**
 * HTTP error body for {@link SerialGatewayApi}: a JSON object {@code {"error":"..."}}.
 * The message is the only field. {@code BoardResponse.error} is the board's message list
 * on a successful reply; this type is the HTTP error body.
 */
public record SerialGatewayError(String error) {
}
