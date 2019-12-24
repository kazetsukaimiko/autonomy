package io.freedriver.autonomy.async;

import kaze.victron.VEDirectMessage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

@ApplicationScoped
public class VEDirectMessageActor {
    private static final Logger LOGGER = Logger.getLogger(VEDirectMessageActor.class.getName());

    private VEDirectMessage lastMessage;

    public synchronized void actOnVEDirectMessage(@Observes @Default VEDirectMessage veDirectMessage) throws IOException {
        if (lastMessage == null) {
            lastMessage = veDirectMessage;
            LOGGER.info("VE.Direct initial field values: " +
                    VEDirectMessageField.allValues(lastMessage)
            );
        }
        Stream.of(VEDirectMessageField.values())
                .forEach(field -> compareMessageField(field, veDirectMessage));
    }

    private void compareMessageField(VEDirectMessageField field, VEDirectMessage newMessage) {
        String oldField = field.apply(lastMessage);
        String newField = field.apply(newMessage);
        if (!Objects.equals(oldField, newField)) {
            LOGGER.info(field.getFieldName() + " changed: "
                + oldField + " -> " + newField);
        }
    }

}
