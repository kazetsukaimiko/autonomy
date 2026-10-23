package io.freedriver.autonomy.mqtt;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.freedriver.autonomy.service.SimpleAliasService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Applies a command to the mapped board for that appliance name. An unknown
 * name returns false and does not call {@link SimpleAliasService#setState}.
 */
@ApplicationScoped
public class MappedApplianceCommands implements ApplianceCommandSink {

    private final SimpleAliasService aliases;
    private final CacheApplianceStateSource states;

    @Inject
    public MappedApplianceCommands(SimpleAliasService aliases, CacheApplianceStateSource states) {
        this.aliases = aliases;
        this.states = states;
    }

    @Override
    public boolean apply(String applianceName, boolean on) throws IOException {
        Optional<UUID> boardId = states.boardFor(applianceName);
        if (boardId.isEmpty()) {
            return false;
        }
        aliases.setState(boardId.get(), Map.of(applianceName, on));
        return true;
    }
}
