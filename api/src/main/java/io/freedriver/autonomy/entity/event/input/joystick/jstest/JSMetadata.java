package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class JSMetadata {
    private Path path;
    private String title;
    private Map<Integer, String> axisNames = new HashMap<>();
    private Map<Integer, String> buttonNames = new HashMap<>();

    public JSMetadata(Path path) {
        this.path = path;
    }

    public JSMetadata() {

    }

    public static void index(String source, BiConsumer<Integer, String> putter) {
        index(source.split(",\\s*"), putter);
    }

    public static void index(String[] source, BiConsumer<Integer, String> putter) {
        for(int i = 0; i<source.length; i++) {
            putter.accept(i, source[i]);
        }
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
        return Objects.equals(path, that.path) &&
                Objects.equals(title, that.title) &&
                Objects.equals(axisNames, that.axisNames) &&
                Objects.equals(buttonNames, that.buttonNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, title, axisNames, buttonNames);
    }

    @Override
    public String toString() {
        return "JSMetadata{" +
                "path=" + path +
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