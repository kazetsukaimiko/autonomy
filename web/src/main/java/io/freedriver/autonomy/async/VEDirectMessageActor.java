package io.freedriver.autonomy.async;

import kaze.victron.VEDirectMessage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

@ApplicationScoped
public class VEDirectMessageActor {
    private static final Logger LOGGER = Logger.getLogger(VEDirectMessageActor.class.getName());

    private VEDirectMessage lastMessage;

    public synchronized void actOnVEDirectMessage(@Observes @Default VEDirectMessage veDirectMessage) throws IOException {
        if (lastMessage != null) {
            Stream.of(VEDirectMessageChange.values())
                    .forEach(field -> compareMessageField(field, veDirectMessage));
        } else {
            LOGGER.info("VE.Direct initial field values: " +
                    VEDirectMessageChange.allValues(veDirectMessage));
        }
        lastMessage = veDirectMessage;
    }

    private void compareMessageField(VEDirectMessageChange field, VEDirectMessage newMessage) {
        field.test(lastMessage, newMessage, LOGGER::info);
    }

}
