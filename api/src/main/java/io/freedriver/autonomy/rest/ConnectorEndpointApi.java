package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.entity.jsonlink.BoardEntity;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.GroupEntity;
import io.freedriver.autonomy.entity.jsonlink.PinEntity;
import org.dizitart.no2.NitriteId;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
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


    String GROUP_NAME = "groupName";
    String GROUP_NAME_PATH = PIN_GROUP_PATH + "/name/{"+ GROUP_NAME +"}";
    String PERMUTATION_PATH = GROUP_NAME_PATH + "/permutations";

    String GROUP_NEXT_PATH = GROUP_NAME_PATH + "/next";

    //@GET

    @GET
    List<BoardEntity> allBoardNames();

    @GET
    @Path(BOARD_ID_PATH)
    BoardEntity boardById(@PathParam(BOARD_ID) UUID boardId);

    @GET
    @PathParam(PIN_GROUP_PATH)
    List<GroupEntity> pinGroupsByBoardId(@PathParam(BOARD_ID) UUID boardId);

    @GET
    @PathParam(PIN_NAMES_PATH)
    List<PinEntity> pinNamesByBoardId(@PathParam(BOARD_ID) UUID boardId);

    @GET
    @PathParam(PERMUTATION_PATH)
    List<PermutationEntity> permutationsOfGroup(@PathParam(BOARD_ID) UUID boardId, @PathParam(GROUP_NAME) String groupName);

    @GET
    @Path(GROUP_NAME_PATH)
    PermutationEntity readGroup(@PathParam(BOARD_ID) UUID boardId, @PathParam(GROUP_NAME) String groupName);

    @GET
    @Path(GROUP_NEXT_PATH)
    PermutationEntity cycleGroup(@PathParam(BOARD_ID) UUID boardId, @PathParam(GROUP_NAME) String groupName);

}
