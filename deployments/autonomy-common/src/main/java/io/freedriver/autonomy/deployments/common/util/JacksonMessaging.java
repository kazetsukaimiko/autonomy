package io.freedriver.autonomy.deployments.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

public class JacksonMessaging implements MessageConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    @Override
    public byte[] toMessage(Object o) throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(new JacksonPayload<>(o));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromMessage(byte[] data) throws IOException {
        return (T) OBJECT_MAPPER.readValue(data, JacksonPayload.class)
                .reify();
    }

    private static final class JacksonPayload<T> {
        private Class<T> entityType;
        private JsonNode entityContent;

        public JacksonPayload() {
        }

        public JacksonPayload(Class<T> entityType, JsonNode node) {
            this.entityType = entityType;
            this.entityContent = node;
        }

        @SuppressWarnings("unchecked")
        public JacksonPayload(T entity) {
            this((Class<T>) entity.getClass(), OBJECT_MAPPER.valueToTree(entity));
        }

        public Class<T> getEntityType() {
            return entityType;
        }

        public void setEntityType(Class<T> entityType) {
            this.entityType = entityType;
        }

        public JsonNode getEntityContent() {
            return entityContent;
        }

        public void setEntityContent(JsonNode entityContent) {
            this.entityContent = entityContent;
        }

        public T reify() throws JsonProcessingException {
            return OBJECT_MAPPER.treeToValue(entityContent, getEntityType());
        }
    }
}
