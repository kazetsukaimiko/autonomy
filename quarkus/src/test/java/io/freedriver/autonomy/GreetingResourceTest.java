package io.freedriver.autonomy;


import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
public class GreetingResourceTest {

    @Test
    public void testHelloEndpoint() {
        /*
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello RESTEasy"));

         */
    }

}