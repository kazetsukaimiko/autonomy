package io.freedriver.autonomy.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.stream.Stream;

public interface ReadEndpoint<ENTITY, ID> {
    String ID_PARAMETER = "id";

    @GET
    Stream<ENTITY> findAll();

    @GET
    @Path("/id/{"+ID_PARAMETER+"}")
    ENTITY findOne(ID id);
}
