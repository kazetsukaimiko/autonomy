package io.freedriver.autonomy.scheduled;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * One {@link SimpleAliasService#setState} call. Applied when the response shows
 * every requested output in the requested state.
 */
@ApplicationScoped
public class AliasBoardGateway implements CommandListEngine.BoardGateway {

    private final SimpleAliasService aliases;

    @Inject
    public AliasBoardGateway(SimpleAliasService aliases) {
        this.aliases = aliases;
    }

    @Override
    public CommandListEngine.WriteResult write(UUID board, Map<String, Boolean> desired) {
        try {
            Response response = aliases.setState(board, desired);
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
