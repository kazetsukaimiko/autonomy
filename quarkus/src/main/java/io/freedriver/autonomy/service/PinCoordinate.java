package io.freedriver.autonomy.service;

import java.util.UUID;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import lombok.Builder;

@Builder(toBuilder = true)
public record PinCoordinate(UUID boardId, Identifier identifier) {
}
