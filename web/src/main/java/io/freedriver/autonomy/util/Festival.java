package io.freedriver.autonomy.util;

import java.io.IOException;

public class Festival {
    private Festival() {

    }


    public static int speak(String phrase) {
        try {
            final Process process = new ProcessBuilder("festival", "--tts").start();
            process.getOutputStream().write(phrase.getBytes());
            process.getOutputStream().close();
            return process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
