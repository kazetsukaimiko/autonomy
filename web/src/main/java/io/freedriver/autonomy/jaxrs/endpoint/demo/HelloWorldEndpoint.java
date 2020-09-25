package io.freedriver.autonomy.jaxrs.endpoint.demo;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/hello")
public class HelloWorldEndpoint {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Person doGet() {
		return new Person("Scott", 33);
	}
}
