package io.freedriver.autonomy.cdi.qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ByUUID {
    @Nonbinding
    String value();

    final class Literal extends AnnotationLiteral<ByUUID> implements ByUUID {

        private final UUID uuid;

        public Literal(UUID uuid) {
            this.uuid = uuid;
        }

        public Literal(String uuid) {
            this(UUID.fromString(uuid));
        }

        @Override
        public String value() {
            return uuid.toString();
        }

        public static ByUUID.Literal ofUUID(UUID uuid) {
            return new Literal(uuid);
        }
    }

}
