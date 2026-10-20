# Changelog

`GET /api/changelog` serves the changelog packaged in the autonomy jar as `text/plain`.

The file is the classpath resource `changelog/CHANGELOG.md` (`io.freedriver.autonomy.changelog.Changelog`). The Maven build runs git-cliff and writes it to `quarkus/target/generated-changelog/CHANGELOG.md` (gitignored, under `target/`), then copies it into the jar at package time. The path is outside `META-INF/resources`, so Quarkus does not serve it as a static file.

git-cliff reads conventional commits up to the current release tag (`YYYY-MM_rN`) and omits commits after that tag. A build with no reachable tag, such as a shallow clone, packages no file. The endpoint then returns 404.

The endpoint takes no parameters and does not build a file path from the request. It reads only that fixed classpath resource.
