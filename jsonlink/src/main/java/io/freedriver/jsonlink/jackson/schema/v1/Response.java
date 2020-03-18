package io.freedriver.jsonlink.jackson.schema.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.freedriver.jsonlink.jackson.schema.base.BaseResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class Response extends BaseResponse {
    private UUID uuid;
    private UUID requestId;
    private List<String> error = new ArrayList<>();
    private Map<Identifier, Boolean> digital;
    private List<AnalogResponse> analog;

    @JsonIgnore
    private final Instant created = Instant.now();

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public List<String> getError() {
        return error;
    }

    public void setError(List<String> error) {
        this.error = error;
    }

    public Map<Identifier, Boolean> getDigital() {
        return digital != null ? digital : Collections.emptyMap();
    }

    public void setDigital(Map<Identifier, Boolean> digital) {
        this.digital = digital;
    }

    public List<AnalogResponse> getAnalog() {
        return analog != null ? analog : Collections.emptyList();
    }

    public void setAnalog(List<AnalogResponse> analog) {
        this.analog = analog;
    }

    public Response logAnyErrors(Consumer<String> errorLogger) {
        getError().forEach(errorLogger);
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return "Response{" +
                "uuid=" + uuid +
                ", error=" + error +
                ", digital=" + digital +
                ", analog=" + analog +
                '}';
    }
}
