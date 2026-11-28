package io.freedriver.autonomy.scheduled;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.jsonlink.api.v1.BoardResponse;
import io.freedriver.jsonlink.api.v1.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * One {@link SimpleAliasService#setState} call. Applied when the response shows
 * every requested output in the requested state.
 */
@ApplicationScoped
public class AliasBoardGateway implements CommandListEngine.BoardGateway {

    private final SimpleAliasService aliases;
    private final ConnectorService connectors;

    @Inject
    public AliasBoardGateway(SimpleAliasService aliases, ConnectorService connectors) {
        this.aliases = aliases;
        this.connectors = connectors;
    }

    @Override
    public CommandListEngine.WriteResult write(UUID board, Map<String, Boolean> desired) {
        try {
            BoardResponse response = aliases.setState(board, desired);
            // The response from a send that ran before startup actions, or while they
            // are still running, is not confirmation. The off stays pending and retries.
            if (!connectors.startupActionsFinished(board) || connectors.lastSendPrecededStartupActions()) {
                return CommandListEngine.WriteResult.FAILED;
            }
            Map<Identifier, Boolean> pins = aliases.identifiers(board, desired);
            Map<Identifier, Boolean> digital = response.digital();
            if (digital == null || digital.isEmpty()) {
                return CommandListEngine.WriteResult.FAILED;
            }
            for (Map.Entry<Identifier, Boolean> pin : pins.entrySet()) {
                if (!pin.getValue().equals(digital.get(pin.getKey()))) {
                    return CommandListEngine.WriteResult.FAILED;
                }
            }
            return CommandListEngine.WriteResult.APPLIED;
        } catch (RuntimeException | IOException e) {
            return CommandListEngine.WriteResult.FAILED;
        }
    }
}
