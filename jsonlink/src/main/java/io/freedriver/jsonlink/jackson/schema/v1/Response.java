package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class Response {
    private UUID uuid;
    private List<String> error = new ArrayList<>();
    private Map<Identifier, Boolean> digital;
    private List<AnalogResponse> analog;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public List<String> getError() {
        return error;
    }

    public void setError(List<String> error) {
        this.error = error;
    }

    public Map<Identifier, Boolean> getDigital() {
        return digital;
    }

    public void setDigital(Map<Identifier, Boolean> digital) {
        this.digital = digital;
    }

    public List<AnalogResponse> getAnalog() {
        return analog;
    }

    public void setAnalog(List<AnalogResponse> analog) {
        this.analog = analog;
    }

    public Response logAnyErrors(Consumer<String> errorLogger) {
        getError().forEach(errorLogger);
        return this;
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
