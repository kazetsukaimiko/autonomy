package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.entity.jsonlink.BoardEntity;
import io.freedriver.autonomy.entity.jsonlink.GroupEntity;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinEntity;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.config.Operation;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.freedriver.jsonlink.config.Operation.ON;

@RequestScoped
public class ConnectorEndpoint implements ConnectorEndpointApi {

    @Inject
    private ConnectorService connectorService;

    @Override
    public List<BoardEntity> allBoardNames() {
        return connectorService.getWorkspace()
                .getBoards();
    }

    @Override
    public BoardEntity boardById(UUID boardId) {
        return connectorService.getWorkspace()
                .getBoards().stream()
                .filter(boardEntity -> Objects.equals(boardId, boardEntity.getBoardId()))
                .findFirst()
                .orElseThrow(() -> new WebApplicationException(
                        "Board Id " + boardId.toString() + " not found. Available Boards: " + connectorService.describeBoards(), 404));
    }

    @Override
    public Map<Identifier, Boolean> manualControl(UUID boardId, Operation operation, String pins) {
        // Get all configured pins for the board.
        Set<Identifier> configuredPins = boardById(boardId)
                .getPins()
                .stream()
                .map(PinEntity::getPin)
                .collect(Collectors.toSet());

        // Read the current state of these pins.
        Map<Identifier, Boolean> currentState = connectorService.readDigital(boardId, configuredPins);

        // Set the pins
        return connectorService.send(boardId, split(pins)
                .map(Integer::parseInt)
                .map(Identifier::new)
                .filter(configuredPins::contains)
                .flatMap(identifier -> operation.fromDifferent(currentState.get(identifier))
                        .map(identifier::setDigital).stream())
                .reduce(new Request(), Request::digitalWrite, (a, b) -> a))
                .getDigital();
    }

    @Override
    public List<GroupEntity> pinGroupsByBoardId(UUID boardId) {
        return boardById(boardId)
                .getGroups();
    }

    @Override
    public List<PinEntity> pinNamesByBoardId(UUID boardId) {
        return boardById(boardId)
                .getPins();
    }

    @Override
    public List<PermutationEntity> permutationsOfGroup(UUID boardId, String groupName) {
        return getGroup(boardId, groupName)
                .getPermutations();
    }

    @Override
    public PermutationEntity readGroup(UUID boardId, String groupName) {
        return connectorService.currentPermutation(boardId, getGroup(boardId, groupName))
                .orElseThrow(() -> new WebApplicationException("Couldn't resolve Permutation of group: "+groupName));
    }

    @Override
    public PermutationEntity cycleGroup(UUID boardId, String groupName) {
        return connectorService.nextPermutation(boardId, getGroup(boardId, groupName))
                .orElseThrow(() -> new WebApplicationException("Couldn't resolve Permutation of group: "+groupName));
    }

    private GroupEntity getGroup(UUID boardId, String groupName) {
        return pinGroupsByBoardId(boardId).stream()
                .filter(groupEntity -> Objects.equals(groupName, groupEntity.getName()))
                .findFirst()
                .orElseThrow(() -> new WebApplicationException(
                        "Group name \"" + groupName + "\" not found.", 404));
    }

}
