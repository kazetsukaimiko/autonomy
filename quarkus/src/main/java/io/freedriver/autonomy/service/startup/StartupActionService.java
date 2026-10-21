package io.freedriver.autonomy.service.startup;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.speech.SpeechEvent;
import io.freedriver.autonomy.event.speech.SpeechEventType;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.MappingsAction;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Runs the board's {@code startupActions} once enumeration finishes.
 * The live list is not in this service. Deploy applies it to the mappings file.
 */
@ApplicationScoped
@Slf4j
public class StartupActionService {
    private final ConnectorService connectors;
    private final SimpleAliasService aliases;
    private final Event<SpeechEvent> speech;
    private final StartupMappingsLoader loader;
    private final StartupActionRunner runner;

    @Inject
    public StartupActionService(
            Validator validator,
            ConnectorService connectors,
            SimpleAliasService aliases,
            Event<SpeechEvent> speech,
            @ConfigProperty(name = "autonomy.mappings.file") String mappingsFile) {
        this(connectors, aliases, speech, new StartupMappingsLoader(validator, Path.of(mappingsFile)), StartupActionRunner.pollingWait());
    }

    public StartupActionService(
            ConnectorService connectors,
            SimpleAliasService aliases,
            Event<SpeechEvent> speech,
            StartupMappingsLoader loader,
            StartupActionRunner.DelayWait wait) {
        this.connectors = connectors;
        this.aliases = aliases;
        this.speech = speech;
        this.loader = loader;
        this.runner = new StartupActionRunner(connectors, wait, this::failed);
    }

    /**
     * The board finished its UUID handshake, including a handshake that completed
     * late. Clears that board's pin cache, then runs its list from the top.
     * A refused mappings file logs and runs nothing.
     */
    public void onBoardEnumerated(UUID boardId) {
        aliases.forgetCachedDigitalPins(boardId);
        StartupMappingsLoader.Loaded loaded;
        try {
            loaded = loader.load();
        } catch (RuntimeException e) {
            log.error("startupActions refused for board {}", boardId, e);
            return;
        }
        if (loaded.fileRefused()) {
            return;
        }
        List<MappingsAction> actions = loaded.actions().getOrDefault(boardId.toString(), List.of());
        if (actions.isEmpty()) {
            return;
        }
        log.info("Running {} startupActions for board {}", actions.size(), boardId);
        runner.run(boardId, actions, new BoardGpio(boardId), new BoardAppliances(boardId), () -> connectors.isBoardConnected(boardId));
    }

    private void failed(UUID boardId, String message) {
        log.error("startupActions for board {}: {}", boardId, message);
        if (speech == null) {
            return;
        }
        try {
            speech.fire(new SpeechEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    GenerationOrigin.NON_HUMAN,
                    StartupActionService.class.getName(),
                    boardId.toString(),
                    null,
                    SpeechEventType.WARNING,
                    "startupActions",
                    message));
        } catch (RuntimeException e) {
            log.warn("startupActions notification failed", e);
        }
    }

    private final class BoardGpio implements GpioControl {
        private final UUID boardId;

        private BoardGpio(UUID boardId) {
            this.boardId = boardId;
        }

        @Override
        public void setPin(Identifier pin, boolean on) {
            Request request = Request.empty().digitalWrite(new DigitalWrite(pin, on));
            Response response = connectors.send(boardId, request);
            // A send that times out throws before this. The pin cache stays unchanged.
            if (response != null && !response.digital().isEmpty()) {
                aliases.cacheBoardDigitalState(boardId, response.digital());
            }
        }
    }

    private final class BoardAppliances implements ApplianceControl {
        private final UUID boardId;

        private BoardAppliances(UUID boardId) {
            this.boardId = boardId;
        }

        @Override
        public Identifier pin(String appliance) {
            Mapping mapping;
            try {
                mapping = aliases.getMapping(boardId);
            } catch (IOException e) {
                throw new IllegalStateException("couldn't read mappings for board " + boardId, e);
            }
            return mapping.appliances().stream()
                    .filter(mapped -> Objects.equals(appliance, mapped.name()))
                    .map(Appliance::identifier)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "no appliance '" + appliance + "' on board " + boardId));
        }
    }
}
