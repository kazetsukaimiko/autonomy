package io.freedriver.autonomy.mqtt.contract;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Shared MQTT appliance JSON contract. The STRICT mapper is local to this
 * module so consumers keep their own ObjectMapper settings.
 *
 * {@code name} is the existing alias key (same string as AliasView.applianceStates).
 * It is not a slug: underscores and mixed case are allowed.
 */
public final class ApplianceSchemas {

    public static final int SCHEMA_VERSION = 1;
    public static final int NAME_MAX = 64;

    public static final String STATE_TOPIC = "freedriver/v1/home/appliances";
    public static final String COMMAND_TOPIC = "freedriver/v1/home/commands";
    public static final int QOS = 1;
    public static final boolean RETAIN = false;

    public static final String APPLIANCES_SCHEMA = "/schema/v1/appliances.json";
    public static final String COMMANDS_SCHEMA = "/schema/v1/commands.json";

    public static final ObjectMapper STRICT = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private ApplianceSchemas() {
    }

    public static boolean validName(String name) {
        return name != null && !name.isBlank() && name.length() <= NAME_MAX;
    }
}

