package io.freedriver.autonomy.jaxrs.endpoint;

public interface CRUDApi<ENTITY, ID> extends CreateApi<ENTITY, ID>, ReadApi<ENTITY, ID> {
}
