package io.freedriver.autonomy.jpa.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Base64Serialization {
    private static final Logger LOGGER = Logger.getLogger(Base64Serialization.class.getName());
    private Base64Serialization() {

    }

    public static Optional<String> encode(Serializable serializable) {
        try {
            return Optional.ofNullable(base64Encode(serializable));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed serialization", e);
            return Optional.empty();
        }
    }

    public static <T> Optional<T> decode(String base64String, Class<T> targetKlazz) {
        try {
            return base64Decode(base64String, targetKlazz);
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Failed deserialiation:", e);
            return Optional.empty();
        }
    }

    private static String base64Encode(Serializable serializable) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.flush();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static <T> Optional<T> base64Decode(String base64String, Class<T> targetKlazz) throws IOException, ClassNotFoundException {
        if (base64String == null) {
            return Optional.empty();
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(base64String.getBytes());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return Optional.ofNullable(ois.readObject())
                .filter(targetKlazz::isInstance)
                .map(targetKlazz::cast);
    }
}
