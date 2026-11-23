package io.freedriver.autonomy.serial;

import java.util.UUID;

/**
 * One board whose UUID handshake has completed.
 *
 * @param boardId the board UUID from the handshake
 * @param device the serial device path the gateway opened
 * @param connectionId a new random UUID minted for this handshake. The gateway keeps no counter.
 *                     A connectionId the controller has not seen for this board means the board
 *                     reconnected, and the controller runs its startup path.
 */
public record BoardView(UUID boardId, String device, UUID connectionId) {
}
