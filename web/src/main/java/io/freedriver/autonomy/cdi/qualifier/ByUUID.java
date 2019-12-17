package io.freedriver.autonomy.cdi.qualifier;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.util.UUID;

@Qualifier
public @interface ByUUID {
    String value();

    final class Literal extends AnnotationLiteral<ByUUID> implements ByUUID {

        private final UUID uuid;

        public Literal(UUID uuid) {
            this.uuid = uuid;
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
