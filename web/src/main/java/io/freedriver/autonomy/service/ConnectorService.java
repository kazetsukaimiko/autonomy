package io.freedriver.autonomy.service;

import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.config.ConnectorConfig;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConnectorService {
    private static final Logger LOGGER = Logger.getLogger(ConnectorService.class.getName());

    public Set<Connector> getAllConnectors() {
        return Connectors.allConnectors()
                .collect(Collectors.toSet());
    }




    // hallway, bathroom
    public Optional<Response> cyclePinGroup(String groupName) {
        return Optional.empty();
    }

    public Optional<Response> readPinGroup(String groupName) {
        return Optional.empty();
    }


    /*
    public List<Permutation> getGroups() {
        return configuration.getGroups();
    }

    // hallway, bathroom
    public Optional<Response> cyclePinGroup(String groupName) {
        return pinGroupByName(groupName)
                .flatMap(pinGroup -> readAllDigitalPins()
                                .map(response -> nextPermutation(pinGroup, response.getDigital())))
                .flatMap(this::sendRequest);
    }

    public Optional<Permutation> pinGroupByName(String groupName) {
        return configuration.getGroups()
                .stream()
                // TODO: Get the group from the configuration looked up by controller.button name
                .filter(pg -> Objects.equals(groupName, pg.getName()))
                .findFirst();
    }

    public Optional<Response> readPinGroup(String groupName) {
        return pinGroupByName(groupName).flatMap(this::readPinGroup);
    }

    public Optional<Response> readPinGroup(Permutation pinGroup) {
        Request request = new Request();
        request.digitalRead(pinGroup.getPermutations()
                .stream()
                .map(Map::keySet)
                .flatMap(Set::stream)
                .distinct()
                .flatMap(alias -> configuration
                                .getPins()
                                .entrySet()
                                .stream()
                                .filter(entry -> Objects.equals(alias, entry.getValue()))
                                .map(Map.Entry::getKey))
                                .map(Identifier::new));
        return sendRequest(request);
    }

    public Optional<Response> readAllDigitalPins() {
        Request request = new Request();
        configuration.getIdentifiers()
                .forEach(request::digitalRead);
        return sendRequest(request);
    }

    public Optional<Response> sendRequest(Request request) {
        return sendRequest(() -> request);
    }

    public synchronized Optional<Response> sendRequest(Supplier<Request> requestSupplier) {
        return Optional.of(connector.send(requestSupplier.get()));
    }

    private Request nextPermutation(Permutation pinGroup, Map<Identifier, Boolean> state) {
        int i;
        for(i=0; i<pinGroup.getPermutations().size(); i++) {
            if (!comparePermutation(pinGroup.getPermutations().get(i), state)) {
                break;
            }
        }
        return pinGroup.getPermutations().get(i==pinGroup.getPermutations().size() ? 0 : i)
                .entrySet()
                .stream()
                .reduce(
                        new Request(),
                        (request, entry) -> request.digitalWrite(ofAlias(entry.getKey()).setDigital(entry.getValue())),
                        (a,b) -> a);

    }

    private boolean comparePermutation(Map<String, Boolean> permutation, Map<Identifier, Boolean> state) {
        Map<Identifier, Boolean> translated = permutation.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> ofAlias(e.getKey()),
                        Map.Entry::getValue,
                        (a, b) -> a
                ));
        return translated.keySet().stream()
                .allMatch(identifier -> state.containsKey(identifier) && Objects.equals(translated.get(identifier), state.get(identifier)));
    }

    private Optional<String> getAlias(Identifier identifier) {
        return getAlias(identifier.getPin());
    }

    private Optional<String> getAlias(int pinValue) {
        return Optional.ofNullable(configuration.getPins().getOrDefault(pinValue, null));
    }


    private Identifier ofAlias(String alias) {
        return configuration.getPins().entrySet()
                .stream()
                .filter(e -> Objects.equals(alias, e.getValue()))
                .map(Map.Entry::getKey)
                .map(Identifier::new)
                .findFirst()
                .orElse(null);
    }

*/
}
