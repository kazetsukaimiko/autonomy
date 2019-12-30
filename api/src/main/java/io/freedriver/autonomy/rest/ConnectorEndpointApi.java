package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.entity.jsonlink.BoardNameEntity;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinGroupEntity;
import io.freedriver.autonomy.entity.jsonlink.PinNameEntity;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import org.dizitart.no2.NitriteId;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Path(ConnectorEndpointApi.ROOT)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ConnectorEndpointApi {
    String ROOT = "/connectors";
    String BOARD_ID = "board";
    String BOARD_ID_PATH = "/id/{"+BOARD_ID+"}";
    String PIN_NAMES_PATH = BOARD_ID_PATH + "/pinNames";
    String PIN_GROUP_PATH = BOARD_ID_PATH + "/pinGroups";
    String PERMUTATION_PATH = BOARD_ID_PATH + "/permutations";

    String GROUP_ID = "groupId";
    String GROUP_ID_PATH = PIN_GROUP_PATH + "/id/{"+GROUP_ID+"}";
    String GROUP_NEXT_PATH = GROUP_ID_PATH + "/next";


    /*
    Matching methods: [
    public abstract java.util.List io.freedriver.autonomy.rest.ConnectorEndpointApi.pinGroupsByBoardId(java.util.UUID),
    public abstract java.util.List io.freedriver.autonomy.rest.ConnectorEndpointApi.permutationsByBoardId(java.util.UUID),
    public abstract java.util.List io.freedriver.autonomy.rest.ConnectorEndpointApi.allBoardNames(),
    public abstract java.util.List io.freedriver.autonomy.rest.ConnectorEndpointApi.pinNamesByBoardId(java.util.UUID)
    */
    @GET
    List<BoardNameEntity> allBoardNames();

    @GET
    @Path(BOARD_ID_PATH)
    BoardNameEntity boardById(@PathParam(BOARD_ID) UUID boardId);

    @GET
    @PathParam(PIN_GROUP_PATH)
    List<PinGroupEntity> pinGroupsByBoardId(@PathParam(BOARD_ID) UUID boardId);

    @GET
    @PathParam(PIN_NAMES_PATH)
    List<PinNameEntity> pinNamesByBoardId(@PathParam(BOARD_ID) UUID boardId);

    @GET
    @PathParam(PERMUTATION_PATH)
    List<PermutationEntity> permutationsByBoardId(@PathParam(BOARD_ID) UUID boardId);

    @GET
    @Path(GROUP_ID_PATH)
    PermutationEntity readGroup(@PathParam(BOARD_ID) UUID boardId, @PathParam(GROUP_ID) NitriteId groupNitriteId);

    @GET
    @Path(GROUP_NEXT_PATH)
    PermutationEntity cycleGroup(@PathParam(BOARD_ID) UUID boardId, @PathParam(GROUP_ID) NitriteId groupNitriteId);

}
