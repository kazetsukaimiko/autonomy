package io.freedriver.autonomy.changelog;

import java.util.Optional;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * {@code GET /api/changelog} serves the packaged changelog as plain text.
 * The route is a constant. The request path and query are never used to choose a file.
 */
@ApplicationScoped
public class ChangelogEndpoint {

    private final Changelog changelog;

    @Inject
    public ChangelogEndpoint(Changelog changelog) {
        this.changelog = changelog;
    }

    public void register(@Observes Router router) {
        router.get("/api/changelog").handler(this::serve);
    }

    void serve(RoutingContext context) {
        Optional<String> body = changelog.text();
        if (body.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }
        context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "text/plain;charset=UTF-8")
                .end(body.get());
    }
}
