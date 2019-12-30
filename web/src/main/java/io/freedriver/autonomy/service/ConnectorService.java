package io.freedriver.autonomy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.autonomy.cdi.provider.ConfigurationProvider;
import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.entity.jsonlink.BoardNameEntity;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinGroupEntity;
import io.freedriver.autonomy.entity.jsonlink.PinNameEntity;
import io.freedriver.autonomy.iface.Positional;
import io.freedriver.autonomy.service.crud.BoardNameService;
import io.freedriver.autonomy.service.crud.NitriteCRUDService;
import io.freedriver.autonomy.service.crud.PermutationService;
import io.freedriver.autonomy.service.crud.PinGroupService;
import io.freedriver.autonomy.service.crud.PinNameService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.config.Mappings;
import io.freedriver.jsonlink.config.PinName;
import io.freedriver.jsonlink.jackson.schema.v1.ModeSet;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import org.dizitart.no2.NitriteId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.freedriver.jsonlink.jackson.schema.v1.Mode.OUTPUT;

@ApplicationScoped
public class ConnectorService {
    private static final Logger LOGGER = Logger.getLogger(ConnectorService.class.getName());
    private static final Set<Connector> ACTIVE_CONNECTORS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/autonomy");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);


    @Inject
    private BoardNameService boardNameService;

    @Inject
    private PinNameService pinNameService;

    @Inject
    private PinGroupService groupService;

    @Inject
    private PermutationService permutationService;

    public Stream<PinNameEntity> pinNamesByPinGroup(PinGroupEntity pinGroup) {
        return pinNameService.getAllById(pinGroup.getPinIds().toArray(NitriteId[]::new));
    }

    public PermutationEntity initialState(PinGroupEntity pinGroupEntity) {
        List<PermutationEntity> allPermutations = permutationService.byPinGroup(pinGroupEntity)
                .sorted(Positional.EXPLICIT_ORDER)
                .collect(Collectors.toList());

        // Since we fetched with the correct order explicitly, we need to re-ensure these have an order.
        if (!Positional.correctOrder(allPermutations)) {
            allPermutations = permutationService.saveOrder(allPermutations)
                    .collect(Collectors.toList());
        }

        return allPermutations.stream()
                .filter(PermutationEntity::isInitialState)
                .findFirst()
                .orElse(allPermutations.isEmpty() ? null : allPermutations.get(0));
    }

    public PermutationEntity currentPermutation(PinGroupEntity pinGroupEntity) {
        return readPinGroup(pinGroupEntity)
                .flatMap(response -> matchPermutation(response, pinGroupEntity))
                .orElse(initialState(pinGroupEntity));
    }

    public PermutationEntity nextPermutation(PinGroupEntity pinGroupEntity) {
        return Positional.next(permutationService.byPinGroup(pinGroupEntity)
                .sorted(Positional.EXPLICIT_ORDER)
                .collect(Collectors.toList()), currentPermutation(pinGroupEntity));
    }

    public Optional<PermutationEntity> matchPermutation(Response response, PinGroupEntity pinGroupEntity) {
        Set<PinNameEntity> pinNames = pinNamesByPinGroup(pinGroupEntity)
                .collect(Collectors.toSet());
        return permutationService.byPinGroup(pinGroupEntity)
                .filter(permutation -> pinGroupEntity.match(response, permutation, pinNames::stream))
                .findFirst();
    }

    public Optional<PermutationEntity> cyclePinGroup(PinGroupEntity pinGroupEntity) {
        Set<PinNameEntity> pinNames = pinNamesByPinGroup(pinGroupEntity)
                .collect(Collectors.toSet());
        return getConnectorByBoardId(pinGroupEntity.getBoardId())
                .map(connector -> pinGroupEntity.apply(connector, nextPermutation(pinGroupEntity), pinNames::stream));
    }

    public List<PinGroupEntity> pinGroupsByBoardId(UUID boardId) {
        return groupService.findByBoardId(boardId)
                .collect(Collectors.toList());
    }

    public List<PinNameEntity> pinNamesByBoardId(UUID boardId) {
        return pinNameService.findByBoardId(boardId)
                .collect(Collectors.toList());
    }

    public List<PermutationEntity> permutationsByBoardId(UUID boardId) {
        return permutationService.findByBoardId(boardId)
                .collect(Collectors.toList());
    }

    public List<BoardNameEntity> allBoardNames() {
        return getAllConnectors().stream()
                .map(boardNameService::findOrCreate)
                .collect(Collectors.toList());
    }

    public Optional<Response> readPinGroup(PinGroupEntity pinGroup) {
        return getConnectorByBoardId(pinGroup.getBoardId())
                .map(connector -> connector.send(new Request()
                        .digitalRead(pinNamesByPinGroup(pinGroup).map(PinNameEntity::getPinNumber))));
    }

    /*
     * INTERNALS / HELPERS
     */

    private Set<Connector> getAllConnectors() {
        Connectors.allConnectors()
                .filter(connector -> !ACTIVE_CONNECTORS.contains(connector))
                .map(this::activateConnector)
                .forEach(ACTIVE_CONNECTORS::add);
        return ACTIVE_CONNECTORS;
    }

    private Connector activateConnector(Connector connector) {
        UUID boardId = connector.getUUID();
        Request request = new Request().modeSet(pinNameService.findByBoardId(boardId)
                .map(pinNameEntity -> new ModeSet(pinNameEntity.getPinNumber(), pinNameEntity.getPinMode())));
        connector.send(request);
        return connector;
    }

    private Optional<Connector> getConnectorByBoardId(UUID boardId) {
        return getAllConnectors().stream()
                .filter(connector -> Objects.equals(boardId, connector.getUUID()))
                .findFirst();
    }

    private Optional<Response> readAllDigitalPins(UUID boardId) {
        return getConnectorByBoardId(boardId)
                .map(connector -> connector.send(
                        new Request().digitalRead(pinNameService.findByBoardId(boardId)
                                .filter(pinNameEntity -> Objects.equals(OUTPUT, pinNameEntity.getPinMode()))
                                .map(PinNameEntity::getPinNumber))));
    }

    public void generateFromMappings() throws IOException {
        Path mappingsFile = inConfigDirectory("mappings.json");
        Mappings mappings = OBJECT_MAPPER.readValue(mappingsFile.toFile(), Mappings.class);

        Stream.of(boardNameService, pinNameService, groupService, permutationService)
                .forEach(NitriteCRUDService::deleteAll);

        mappings.getMappings().forEach(mapping -> {
            // 1: Make board name.
            getConnectorByBoardId(mapping.getConnectorId())
                    .ifPresent(boardNameService::findOrCreate);

            // 2: Make pin names.
            List<PinNameEntity> pinNames = mapping.getPinNamesAsEntities()
                    .stream()
                    .map(pinName -> ofPinName(pinName, mapping.getConnectorId()))
                    .map(pinNameService::save)
                    .collect(Collectors.toList());

            // 3: Make pin groups.
            List<PinGroupEntity> groupEntities = pinNames.stream()
                    .map(PinNameEntity::getPinName)
                    .map(name -> name.contains("_") ? name.split("_")[0] : name)
                    .distinct()
                    .map(groupName -> newPinGroup(groupName, mapping.getConnectorId(), pinNames))
                    .map(groupService::save)
                    .collect(Collectors.toList());

            // 4: Make pin permutations. ALL ON, ALL OFF.
            List<PermutationEntity> permutationEntities = groupEntities.stream()
                    .flatMap(this::permutationsOf)
                    .map(permutationService::save)
                    .collect(Collectors.toList());

        });
    }


    public static Path inConfigDirectory(String name) {
        return Paths.get(CONFIG_PATH.toAbsolutePath().toString(), name);
    }


    public Stream<PermutationEntity> permutationsOf(PinGroupEntity pinGroupEntity) {
        PermutationEntity allOn = new PermutationEntity();
        allOn.setGroupId(pinGroupEntity.getNitriteId());
        allOn.setActivePins(pinGroupEntity.getPinIds());
        allOn.setInactivePins(Collections.emptyList());
        allOn.setInitialState(false);
        allOn.setPosition(0);

        PermutationEntity allOff = new PermutationEntity();
        allOff.setGroupId(pinGroupEntity.getNitriteId());
        allOff.setInactivePins(pinGroupEntity.getPinIds());
        allOff.setActivePins(Collections.emptyList());
        allOff.setInitialState(false);
        allOff.setPosition(1);
        return Stream.of(allOn, allOff);
    }

    public PinGroupEntity newPinGroup(String groupName, UUID boardId, List<PinNameEntity> allPins) {
        PinGroupEntity pinGroupEntity = new PinGroupEntity();
        pinGroupEntity.setName(groupName);
        pinGroupEntity.setBoardId(boardId);
        pinGroupEntity.setPinIds(allPins.stream()
                        .filter(p -> p.getPinName().startsWith(groupName))
                        .map(EntityBase::getNitriteId)
                        .collect(Collectors.toList()));
        pinGroupEntity.setPosition(groupService.findByBoardId(boardId).count());
        return pinGroupEntity;
    }
    public PinNameEntity ofPinName(PinName pinName, UUID boardId) {
        PinNameEntity pinNameEntity = new PinNameEntity();
        pinNameEntity.setBoardId(boardId);
        pinNameEntity.setPinNumber(pinName.getPinNumber());
        pinNameEntity.setPinName(pinName.getPinName());
        pinNameEntity.setPosition(pinNameService.findByBoardId(boardId).count());
        return pinNameEntity;
    }

    public Optional<BoardNameEntity> getBoardById(UUID boardId) {
        return boardNameService.findByBoardId(boardId)
                .findFirst();
    }

}
