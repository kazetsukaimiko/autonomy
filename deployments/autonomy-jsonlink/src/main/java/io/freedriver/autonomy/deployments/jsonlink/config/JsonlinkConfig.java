package io.freedriver.autonomy.deployments.jsonlink.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

import java.nio.file.Path;
import java.util.Set;

@StaticInitSafe // TODO : investigate
@ConfigMapping(prefix = "jsonlink")
public interface JsonlinkConfig {
    Set<Path> connectors();
}
