package io.freedriver.autonomy.rest;

import javax.ws.rs.POST;

public interface CreateApi<ENTITY, ID> {
    @POST
    ID create(ENTITY entity);
}
