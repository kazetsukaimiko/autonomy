package io.freedriver.autonomy.jaxrs.endpoint;

import javax.ws.rs.POST;

public interface CreateApi<ENTITY, ID> {
    @POST
    ID create(ENTITY entity);
}
