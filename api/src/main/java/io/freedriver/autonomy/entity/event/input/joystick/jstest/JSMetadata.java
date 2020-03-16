package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Metadata container for a joystick device.
 */
public class JSMetadata {
    private String title;
    private String hardwareType;
    private Map<Integer, String> axisNames = new HashMap<>();
    private Map<Integer, String> buttonNames = new HashMap<>();

    public JSMetadata() {

    }

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
        for(int i = 0; i<source.length; i++) {
            putter.accept(i, source[i]);
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHardwareType() {
        return hardwareType;
    }

    public void setHardwareType(String hardwareType) {
        this.hardwareType = hardwareType;
    }

    public Map<Integer, String> getAxisNames() {
        return axisNames;
    }

    public void setAxisNames(Map<Integer, String> axisNames) {
        this.axisNames = axisNames;
    }

    public Map<Integer, String> getButtonNames() {
        return buttonNames;
    }

    public void setButtonNames(Map<Integer, String> buttonNames) {
        this.buttonNames = buttonNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSMetadata that = (JSMetadata) o;
        return  Objects.equals(title, that.title) &&
                Objects.equals(axisNames, that.axisNames) &&
                Objects.equals(buttonNames, that.buttonNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, axisNames, buttonNames);
    }

    @Override
    public String toString() {
        return "JSMetadata{" +
                ", title='" + title + '\'' +
                ", axisNames=" + axisNames +
                ", buttonNames=" + buttonNames +
                '}';
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