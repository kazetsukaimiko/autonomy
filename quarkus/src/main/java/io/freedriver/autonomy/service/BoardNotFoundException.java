package io.freedriver.autonomy.service;

/**
 * No open connector has the requested board id.
 */
public class BoardNotFoundException extends RuntimeException {

    public BoardNotFoundException(String message) {
        super(message);
    }
}
