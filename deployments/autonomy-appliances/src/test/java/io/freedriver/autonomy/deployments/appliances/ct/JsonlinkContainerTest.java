package io.freedriver.autonomy.deployments.appliances.ct;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class JsonlinkContainerTest {

    /*
    @Container
    public GenericContainer redis = new GenericContainer(
            new ImageFromDockerfile()
                    .withFileFromString("folder/someFile.txt", "hello")
                    .withFileFromClasspath("test.txt", "mappable-resource/test-resource.txt")
                    .withFileFromClasspath("Dockerfile", "mappable-dockerfile/Dockerfile"))
    )

            .withExposedPorts(6379);

*/
    @Test
    public void testPaths() {
        System.getenv()
                .forEach((k, v) -> System.out.println(k + ":" + v));
    }
}

