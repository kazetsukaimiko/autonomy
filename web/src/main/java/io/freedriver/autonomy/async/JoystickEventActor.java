package io.freedriver.autonomy.async;

import io.freedriver.autonomy.config.Configuration;
import io.freedriver.autonomy.config.PinGroup;
import io.freedriver.autonomy.entity.JoystickEvent;
import io.freedriver.autonomy.util.Festival;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class JoystickEventActor {

    private static final Logger LOGGER = Logger.getLogger(JoystickEventActor.class.getName());

    @Inject
    private Configuration configuration;

    @Inject @Any
    private Instance<Connector> connectors;

    public void actOnEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        if (joystickEvent.getType() == JoystickEvent.Type.BUTTON_UP && !joystickEvent.getInitial()) {
            configuration.getGroups()
                    .stream()
                    // TODO: Get the group from the configuration looked up by controller.button name
                    .filter(pg -> Objects.equals("hallway", pg.getName()))
                    .findFirst()
                    .ifPresent(pinGroup -> {
                        Connector connector = connectors.get();
                        // First get current state
                        Request request = new Request();
                        configuration.getIdentifiers()
                                .forEach(request::digitalRead);

                        Festival.speak(pinGroup.getName());

                        try {
                            // Current state
                            Response response = connector.send(request);
                            // Next state
                            connector.send(nextPermutation(pinGroup, response.getDigital()));
                        } catch (ConnectorException e) {
                            LOGGER.log(Level.WARNING, e, () -> "Couldn't act on Joystick Event.");
                        }

                    });
        }
    }

    private Request nextPermutation(PinGroup pinGroup, Map<Identifier, Boolean> state) {
        int i;
        for(i=0; i<pinGroup.getPermutations().size(); i++) {
            if (comparePermutation(pinGroup.getPermutations().get(i), state)) {
                break;
            }
        }
        return pinGroup.getPermutations().get(i>=pinGroup.getPermutations().size() ? 0 : i)
                .entrySet()
                .stream()
                .reduce(
                        new Request(),
                        (request, entry) -> request.digitalWrite(ofAlias(entry.getKey()).setDigital(entry.getValue())),
                        (a,b) -> a);

    }

    private boolean comparePermutation(Map<String, Boolean> permutation, Map<Identifier, Boolean> state) {
         return permutation.keySet()
                 .stream()
                 .allMatch(alias -> state.keySet().stream()
                    .anyMatch(identifier -> Objects.equals(alias, getAlias(identifier)) &&
                            Objects.equals(permutation.get(alias), state.get(identifier))));
    }

    private Optional<String> getAlias(Identifier identifier) {
        return getAlias(identifier.getPin());
    }

    private Optional<String> getAlias(int pinValue) {
        return Optional.ofNullable(configuration.getAliases().getOrDefault(pinValue, null));
    }


    private Identifier ofAlias(String alias) {
        return configuration.getAliases().entrySet()
                .stream()
                .filter(e -> Objects.equals(alias, e.getValue()))
                .map(Map.Entry::getKey)
                .map(Identifier::new)
                .findFirst()
                .orElse(null);
    }

}

