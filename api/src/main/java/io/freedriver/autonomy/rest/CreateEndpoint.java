package io.freedriver.autonomy.rest;

import javax.ws.rs.POST;

public interface CreateEndpoint<ENTITY, ID> {
    @POST
    ID create(ENTITY entity);
}
