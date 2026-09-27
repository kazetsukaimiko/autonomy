package io.freedriver.autonomy.jaxrs;

import java.util.stream.Stream;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("/stream-serialization")
@Produces(MediaType.APPLICATION_JSON)
public class StreamSerializationResource {

    @GET
    public Stream<String> items() {
        return Stream.of("alpha", "beta", "gamma");
    }
}
