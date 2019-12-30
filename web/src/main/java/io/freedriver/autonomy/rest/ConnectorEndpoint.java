package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.entity.jsonlink.BoardNameEntity;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinGroupEntity;
import io.freedriver.autonomy.entity.jsonlink.PinNameEntity;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.autonomy.service.crud.PinGroupService;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import org.dizitart.no2.NitriteId;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

public class ConnectorEndpoint implements ConnectorEndpointApi {

    @Inject
    private ConnectorService connectorService;

    @Inject
    private PinGroupService groupService;

    @Override
    public List<BoardNameEntity> allBoardNames() {
        return connectorService.allBoardNames();
    }

    @Override
    public List<PinGroupEntity> pinGroupsByBoardId(UUID boardId) {
        return connectorService.pinGroupsByBoardId(boardId);
    }

    @Override
    public List<PinNameEntity> pinNamesByBoardId(UUID boardId) {
        return connectorService.pinNamesByBoardId(boardId);
    }

    @Override
    public List<PermutationEntity> permutationsByBoardId(UUID boardId) {
        return connectorService.permutationsByBoardId(boardId);
    }

    @Override
    public PermutationEntity readGroup(UUID boardId, NitriteId groupNitriteId) {
        return connectorService.currentPermutation(groupService.getById(groupNitriteId));
    }

    @Override
    public PermutationEntity cycleGroup(UUID boardId, NitriteId groupNitriteId) {
        return null;
    }
}
