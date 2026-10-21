package io.freedriver.autonomy.service.startup;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

/** Appliance names on the board whose startup list is running. */
public interface ApplianceControl {
    Identifier pin(String appliance);
}
