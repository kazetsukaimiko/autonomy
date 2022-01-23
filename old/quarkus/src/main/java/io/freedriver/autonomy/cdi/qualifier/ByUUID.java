package io.freedriver.autonomy.cdi.qualifier;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

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
