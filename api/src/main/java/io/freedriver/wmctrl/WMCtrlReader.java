package io.freedriver.wmctrl;

import io.freedriver.util.ProcessUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WMCtrlReader {
    private WMCtrlReader() {
        // No construct
    }

    public static List<WMCtrlEntry> getActiveWindows() {
        try {
            Process process = new ProcessBuilder("wmctrl", "-lG").start();
            return ProcessUtil.linesInputStream(process.getInputStream())
                    .map(WMCtrlEntry::fromString)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read window information", e);
        }
    }

}
