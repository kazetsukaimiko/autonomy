package io.freedriver.autonomy.changelog;

import static io.restassured.RestAssured.given;

import java.util.Optional;
import java.util.Set;

import io.freedriver.autonomy.MappingsTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.junit.jupiter.api.Test;

/** A jar without a changelog file answers 404. */
@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@TestProfile(ChangelogMissingTest.NoChangelogProfile.class)
class ChangelogMissingTest {

    public static class NoChangelogProfile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(NoChangelog.class);
        }
    }

    @Alternative
    @ApplicationScoped
    public static class NoChangelog extends Changelog {
        @Override
        public Optional<String> text() {
            return Optional.empty();
        }
    }

    @Test
    void missing_changelog_is_404() {
        given()
                .when().get("/api/changelog")
                .then()
                .statusCode(404);

        given()
                .when().get("/api/changelog?file=/etc/passwd")
                .then()
                .statusCode(404);
    }
}
