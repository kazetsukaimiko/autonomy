package io.freedriver.autonomy.jaxrs.endpoint;

import java.util.stream.Stream;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public interface ReadApi<ENTITY, ID> {
    String ID_PARAMETER = "id";

    @GET
    Stream<ENTITY> findAll();

    @GET
    @Path("/id/{"+ID_PARAMETER+"}")
    ENTITY findOne(ID id);
}
