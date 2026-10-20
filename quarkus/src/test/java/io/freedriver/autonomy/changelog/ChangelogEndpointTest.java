package io.freedriver.autonomy.changelog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.freedriver.autonomy.MappingsTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Serves src/test/resources/changelog/CHANGELOG.md, the test copy of the file the build packages. */
@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
class ChangelogEndpointTest {

    @Test
    void serves_the_generated_changelog_as_plain_text() throws IOException {
        String fixture = Files.readString(Path.of("src/test/resources/changelog/CHANGELOG.md"));
        assertEquals(fixture, new Changelog().text().orElseThrow());

        given()
                .when().get("/api/changelog")
                .then()
                .statusCode(200)
                .contentType(startsWith("text/plain"))
                .body(is(fixture));

        given()
                .when().get("/api/changelog?file=/etc/passwd")
                .then()
                .statusCode(200)
                .contentType(startsWith("text/plain"))
                .body(is(fixture));
    }

    @Test
    void other_paths_are_not_the_changelog() throws IOException {
        String fixture = Files.readString(Path.of("src/test/resources/changelog/CHANGELOG.md"));

        given()
                .when().get("/changelog/CHANGELOG.md")
                .then()
                .statusCode(not(200));

        given()
                .when().get("/api/changelog/extra")
                .then()
                .body(not(is(fixture)));
    }
}
