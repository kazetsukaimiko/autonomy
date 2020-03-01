package io.freedriver.autonomy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.autonomy.entity.jsonlink.BoardEntity;
import io.freedriver.autonomy.entity.jsonlink.DigitalPin;
import io.freedriver.autonomy.entity.jsonlink.GroupEntity;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinEntity;
import io.freedriver.autonomy.entity.jsonlink.VersionEntity;
import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import io.freedriver.autonomy.iface.Positional;
import io.freedriver.autonomy.service.crud.WorkspaceService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.config.Mappings;
import io.freedriver.jsonlink.config.PinName;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import io.freedriver.jsonlink.jackson.schema.v1.*;
import org.dizitart.no2.NitriteId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The service by which
 */
@ApplicationScoped
public class ConnectorService {
    private static final Logger LOGGER = Logger.getLogger(ConnectorService.class.getName());
    private static final Map<UUID, Connector> ACTIVE_CONNECTORS = new ConcurrentHashMap<>();
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/autonomy");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Inject
    private WorkspaceService workspaceService;

    private WorkspaceEntity workspace;

    public WorkspaceEntity getWorkspace() {
        if (workspace == null) {
            workspaceService.findAll()
                    .filter(WorkspaceEntity::isCurrent)
                    .findFirst()
                    .ifPresentOrElse(this::setWorkspace,
                            () -> workspaceService.findAll()
                                .max(Comparator.comparing(WorkspaceEntity::getVersion))
                                .ifPresent(this::configure));
        }
        return workspace;
    }

    public void setWorkspace(WorkspaceEntity workspace) {
        this.workspace = configure(workspace);
    }

    public Optional<WorkspaceEntity> setWorkspace(NitriteId workspaceId) {
        return workspaceService.findOne(workspaceId)
                .map(this::configure);
    }

    private WorkspaceEntity configure(WorkspaceEntity workspaceEntity) {
        workspaceEntity.setId(workspaceService.save(workspaceEntity).getId());

        getAllConnectors()
                .forEach((boardId, connector) -> configure(connector, workspaceEntity));
        workspaceService.findAll()
                .peek(workspaceEntity1 -> workspaceEntity1.setCurrent((Objects.equals(
                        workspaceEntity.getId(), workspaceEntity1.getId()
                ))))
                .forEach(workspaceService::save);
        this.workspace = workspaceEntity;
        return workspaceEntity;
    }

    private void configure(Connector connector, WorkspaceEntity workspace) {
        workspace.getBoards().stream()
                .filter(boardEntity -> Objects.equals(connector.getUUID(), boardEntity.getBoardId()))
                .findFirst()
                .ifPresent(boardEntity -> setupBoard(connector, boardEntity));

    }

    private void setupBoard(Connector connector, BoardEntity boardEntity) {
        // Setup pins
        connector.send(boardEntity.getPins().stream()
                .map(pinEntity -> new ModeSet(pinEntity.getPin(), pinEntity.getMode()))
                .reduce(new Request(), Request::modeSet, (a, b) -> b));
        // Set pins initial states.
        boardEntity.getGroups()
                .forEach(groupEntity -> setupGroup(connector, groupEntity));
    }

    private void setupGroup(Connector connector, GroupEntity groupEntity) {
        groupEntity.getPermutations().stream()
                .filter(PermutationEntity::isInitialState)
                .findFirst()
                .ifPresent(permutation -> groupEntity.apply(connector, permutation));
    }

    public Optional<PermutationEntity> currentPermutation(UUID boardId, GroupEntity groupEntity) {
        return getConnectorByBoardId(boardId)
                .flatMap(connector -> currentPermutation(connector, groupEntity));
    }

    private Optional<PermutationEntity> currentPermutation(Connector connector, GroupEntity groupEntity) {
        return matchPermutation(groupEntity.read(connector), groupEntity);
    }

    public  Optional<PermutationEntity> nextPermutation(UUID boardId, GroupEntity groupEntity) {
        return getConnectorByBoardId(boardId)
                .flatMap(connector -> nextPermutation(connector, groupEntity));
    }

    private  Optional<PermutationEntity> nextPermutation(Connector connector, GroupEntity groupEntity) {
        return currentPermutation(connector, groupEntity)
                .map(permutationEntity -> Positional.next(groupEntity.getPermutations(), permutationEntity));
    }

    private Optional<PermutationEntity> matchPermutation(Response response, GroupEntity groupEntity) {
        return groupEntity.getPermutations()
                .stream()
                .filter(permutationEntity -> groupEntity.match(response, permutationEntity))
                .findFirst();
    }

    public List<UUID> getConnectedBoards() {
        return new ArrayList<>(getAllConnectors().keySet());
    }

    /*
     * INTERNALS / HELPERS
     */

    private Map<UUID, Connector> getAllConnectors() {
        Connectors.allConnectors()
                .filter(connector -> !ACTIVE_CONNECTORS.containsKey(connector.getUUID()))
                .forEach(connector -> ACTIVE_CONNECTORS.put(connector.getUUID(), connector));
        return ACTIVE_CONNECTORS;
    }


    private Optional<Connector> getConnectorByBoardId(UUID boardId) {
        return getAllConnectors().values().stream()
                .filter(connector -> Objects.equals(boardId, connector.getUUID()))
                .findFirst();
    }

    public WorkspaceEntity generateFromMappings() throws IOException {
        Path mappingsFile = inConfigDirectory("mappings.json");
        Mappings mappings = OBJECT_MAPPER.readValue(mappingsFile.toFile(), Mappings.class);
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setVersion(new VersionEntity());
        workspaceEntity.setName("Generated by mappings.json");

        System.out.println(mappings);

        workspaceEntity.setBoards(mappings.getMappings().stream().flatMap(mapping -> {
            // 1: Make pin names.
            List<PinEntity> pins = mapping.getPinNamesAsEntities()
                    .stream()
                    .map(this::ofPinName)
                    .collect(Collectors.toList());

            System.out.println(pins);


            UUID boardUUID = mapping.getConnectorId();
            if (boardUUID == null) {
                boardUUID = getAllConnectors().values().stream()
                        .map(Connector::getUUID)
                        .findFirst()
                        .orElse(null);
            }

            // 2: Make the board name.
            return getConnectorByBoardId(boardUUID)
                    .map(connector -> {
                        BoardEntity board = new BoardEntity();
                        board.setBoardId(connector.getUUID());
                        board.setName(mapping.getConnectorName());
                        board.setPins(pins);

                        // 3: Make pin groups.
                        List<GroupEntity> groups = pins.stream()
                                .map(PinEntity::getName)
                                .map(name -> name.contains("_") ? name.split("_")[0] : name)
                                .distinct()
                                .map(groupName -> newPinGroup(groupName, pins))
                                .collect(Collectors.toList());

                        groups.forEach(group -> {
                                group.setPins(pins.stream()
                                    .filter(pin -> pin.getName().startsWith(group.getName()))
                                    .collect(Collectors.toList()));
                                group.setPermutations(permutationsOf(group).collect(Collectors.toList()));
                            });
                        board.setGroups(groups);
                        return board;
                    }).stream();
        }).collect(Collectors.toList()));

        // Setup the new workspace.
        return workspaceEntity;
        //configure(workspaceEntity);
    }

    private static Path inConfigDirectory(String name) {
        return Paths.get(CONFIG_PATH.toAbsolutePath().toString(), name);
    }

    private Stream<PermutationEntity> permutationsOf(GroupEntity group) {
        PermutationEntity allOn = new PermutationEntity();
        allOn.setActivePins(group.getPins());
        allOn.setInactivePins(Collections.emptyList());
        allOn.setInitialState(false);
        allOn.setPosition(0);

        PermutationEntity allOff = new PermutationEntity();
        allOff.setInactivePins(group.getPins());
        allOff.setActivePins(Collections.emptyList());
        allOff.setInitialState(false);
        allOff.setPosition(1);
        return Stream.of(allOn, allOff);
    }

    private GroupEntity newPinGroup(String groupName, List<PinEntity> allPins) {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupName);
        groupEntity.setPins(allPins.stream()
                        .filter(p -> p.getName().startsWith(groupName))
                        .collect(Collectors.toList()));
        return groupEntity;
    }

    private PinEntity ofPinName(PinName pinName) {
        PinEntity pinNameEntity = new DigitalPin();
        pinNameEntity.setPin(pinName.getPinNumber());
        pinNameEntity.setName(pinName.getPinName());
        return pinNameEntity;
    }

    public String describeBoards() {
        return getAllConnectors().values().stream()
                .map(Connector::getUUID)
                .sorted(Comparator.comparing(UUID::toString))
                .map(UUID::toString)
                .collect(Collectors.joining(","));
    }

    public synchronized Response send(UUID uuid, Request request) {
        return getConnectorByBoardId(uuid)
                .map(connector -> connector.send(request))
                .orElseThrow(() -> new WebApplicationException("Board not found", 404));
    }

    public synchronized Map<Identifier, Boolean> readDigital(UUID boardId, Collection<Identifier> pins) {
        return send(boardId, pins.stream()
                .reduce(new Request(), Request::digitalRead, (a, b) -> a))
                .getDigital();
    }

    public synchronized Map<Identifier, Boolean> writeDigital(UUID boardId, Map<Identifier, Boolean> state) {
        Request request = new Request();
        state.forEach((pin, pinState) -> request.digitalWrite(new DigitalWrite(pin, pinState)));
        return send(boardId, request)
                .getDigital();
    }
}
