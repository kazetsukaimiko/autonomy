package io.freedriver.autonomy.jaxrs.endpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.stream.Stream;

public interface ReadApi<ENTITY, ID> {
    String ID_PARAMETER = "id";

    @GET
    Stream<ENTITY> findAll();

    @GET
    @Path("/id/{"+ID_PARAMETER+"}")
    ENTITY findOne(ID id);
}
