package io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Metadata container for a joystick device.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class JSMetadata {
    private String title;
    private String hardwareType;
    private String driverVersion;
    private Map<Integer, String> axisNames = new HashMap<>();
    private Map<Integer, String> buttonNames = new HashMap<>();

    /**
     * Simple method to read metadata strings into a JSMetadata container.
     */
    public static void index(String source, BiConsumer<Integer, String> putter) {
        index(source.split(",\\s*"), putter);
    }

    /**
     * Simple method to read metadata strings into a JSMetadata container.
     */
    public static void index(String[] source, BiConsumer<Integer, String> putter) {
        for (int i = 0; i < source.length; i++) {
            putter.accept(i, source[i]);
        }
    }

    // TODO: HATS
    public String getNameOf(JSTestEvent jsTestEvent) {
        if (JSTestEventType.isButton(jsTestEvent.getJsTestEventType())) {
            return getButtonNames().get(jsTestEvent.getNumber().intValue());
        } else {
            return getAxisNames().get(jsTestEvent.getNumber().intValue());
        }
    }
}
