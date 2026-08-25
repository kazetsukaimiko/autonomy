package io.freedriver.autonomy.mqtt.contract;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Shared MQTT appliance JSON contract. Isolation is the autonomy
 * {@code instanceId} (UUIDv4 only), not a board and not the MQTT protocol client-id.
 * {@code instanceName} is UX only: never a topic segment and never in an ACL.
 *
 * Schema 2 is a wire break from v1 ({@code name}, shared {@code /home/} topics).
 * v1 was never live-commanded.
 *
 * Validate {@code instanceId} before it is interpolated into a topic or sent to
 * the broker: standard 8-4-4-4-12 hex + hyphens, version nibble 4. That form
 * also rejects MQTT wildcards {@code /}, {@code +}, and {@code #}.
 *
 * Required / missing / size / version checks are Jakarta Validation after parse.
 * Jackson {@code FAIL_ON_UNKNOWN_PROPERTIES} only rejects extra fields.
 */
public final class ApplianceSchemas {

    public static final int SCHEMA_VERSION = 2;
    public static final int NAME_MAX = 64;

    public static final String APPLIANCES_TOPIC_TEMPLATE = "freedriver/v1/{instanceId}/appliances";
    public static final String COMMANDS_TOPIC_TEMPLATE = "freedriver/v1/{instanceId}/commands";
    public static final String APPLIANCES_WILDCARD = "freedriver/v1/+/appliances";
    public static final String COMMANDS_WILDCARD = "freedriver/v1/+/commands";

    public static final int QOS = 1;
    public static final boolean RETAIN = false;

    public static final String APPLIANCES_SCHEMA = "/schema/v2/appliances.json";
    public static final String COMMANDS_SCHEMA = "/schema/v2/commands.json";

    /**
     * UUIDv4 canonical form only: 8-4-4-4-12 hex + hyphens, version nibble 4,
     * RFC 4122 variant 8/9/a/b. Rejects MQTT {@code /}, {@code +}, {@code #}.
     */
    static final Pattern INSTANCE_ID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    public static final ObjectMapper STRICT = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private ApplianceSchemas() {}

    public static String appliancesTopic(UUID instanceId) {
        return "freedriver/v1/" + requireInstanceId(instanceId) + "/appliances";
    }

    public static String commandsTopic(UUID instanceId) {
        return "freedriver/v1/" + requireInstanceId(instanceId) + "/commands";
    }

    /**
     * {@code instanceId} is the autonomy instance id, not the MQTT protocol client-id.
     * Validate UUIDv4 form before the value is used as a topic segment.
     */
    public static String requireInstanceId(UUID instanceId) {
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId is required");
        }
        return requireSafeTopicSegment(instanceId.toString());
    }

    /**
     * Topic-segment safety and UUIDv4 form. Must run before the id hits the broker.
     */
    public static String requireSafeTopicSegment(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalArgumentException("instanceId is required");
        }
        if (instanceId.indexOf('/') >= 0 || instanceId.indexOf('+') >= 0 || instanceId.indexOf('#') >= 0) {
            throw new IllegalArgumentException(
                    "instanceId must not contain '/', '+', or '#' (not an MQTT client-id)");
        }
        if (!INSTANCE_ID.matcher(instanceId).matches()) {
            throw new IllegalArgumentException("instanceId must be UUIDv4 in 8-4-4-4-12 hex form");
        }
        return instanceId;
    }

    public static boolean validApplianceName(String applianceName) {
        return applianceName != null && !applianceName.isBlank() && applianceName.length() <= NAME_MAX;
    }

    public static <T> T validate(T value) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Rejected MQTT contract: " + detail);
        }
        return value;
    }
}
