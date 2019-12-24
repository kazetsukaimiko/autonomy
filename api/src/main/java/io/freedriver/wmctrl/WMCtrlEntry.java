package io.freedriver.wmctrl;

//import javafx.geometry.Rectangle2D;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WMCtrlEntry {
    private static final Pattern ENTRY = Pattern.compile(String.join("\\s+",
            "(?<wid>0x[0-9a-f]{8})",
            "(?<desktop>\\-?[0-9]*)",
            "(?<xpos>[0-9][0-9]*)",
            "(?<ypos>[0-9][0-9]*)",
            "(?<width>[0-9][0-9]*)",
            "(?<height>[0-9][0-9]*)",
            "(?<source>[\\d\\w][\\d\\w]*)",
            "(?<title>.*)"
    ) +"$");

    private String wid;
    private String desktop;
    private int xpos;
    private int ypos;
    private int width;
    private int height;
    private String source;
    private String title;

    private WMCtrlEntry(String wid, String desktop, int xpos, int ypos, int width, int height, String source, String title) {
        this.wid = wid;
        this.desktop = desktop;
        this.xpos = xpos;
        this.ypos = ypos;
        this.width = width;
        this.height = height;
        this.source = source;
        this.title = title;
    }

    public static Optional<WMCtrlEntry> fromString(String s) {
        return fromMatcher(ENTRY.matcher(s));
    }

    private static Optional<WMCtrlEntry> fromMatcher(Matcher m) {
        if (m.matches()) {
            return Optional.of(new WMCtrlEntry(
                        m.group("wid"),
                        m.group("desktop"),
                        Integer.parseInt(m.group("xpos")),
                        Integer.parseInt(m.group("ypos")),
                        Integer.parseInt(m.group("width")),
                        Integer.parseInt(m.group("height")),
                        m.group("source"),
                        m.group("title")
                    ));
        }
        return Optional.empty();
    }

    /*
    public Rectangle2D getRectangle2D() {
        return new Rectangle2D(getXpos(), getYpos(), getWidth(), getHeight());
    }
     */

    public Rectangle getRectangle() {
        return new Rectangle(getXpos(), getYpos(), getWidth(), getHeight());
    }

    public boolean isDesktop() {
        return getDesktop().startsWith("-");
    }

    public boolean isWindow() {
        return !isDesktop();
    }

    public String getWid() {
        return wid;
    }

    public void setWid(String wid) {
        this.wid = wid;
    }

    public String getDesktop() {
        return desktop;
    }

    public void setDesktop(String desktop) {
        this.desktop = desktop;
    }

    public int getXpos() {
        return xpos;
    }

    public void setXpos(int xpos) {
        this.xpos = xpos;
    }

    public int getYpos() {
        return ypos;
    }

    public void setYpos(int ypos) {
        this.ypos = ypos;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WMCtrlEntry that = (WMCtrlEntry) o;
        return Objects.equals(wid, that.wid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wid);
    }

    @Override
    public String toString() {
        return "WMCtrlEntry{" +
                "wid='" + wid + '\'' +
                ", desktop='" + desktop + '\'' +
                ", xpos=" + xpos +
                ", ypos=" + ypos +
                ", width=" + width +
                ", height=" + height +
                ", source='" + source + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    public boolean nonZero() {
        return getWidth() > 0 && getHeight() > 0;
    }
}
