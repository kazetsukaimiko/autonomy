package io.freedriver.autonomy.jaxrs.endpoint;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public interface ReadApi<ENTITY, ID> {
    String ID_PARAMETER = "id";

    @GET
    List<ENTITY> findAll();

    @GET
    @Path("/id/{"+ID_PARAMETER+"}")
    ENTITY findOne(ID id);
}
