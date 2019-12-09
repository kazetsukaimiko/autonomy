package io.freedriver.autonomy.util;

import java.io.IOException;
import java.net.ServerSocket;

public class Festival extends ProcessSpawner {
    private static final Festival INSTANCE = new Festival();

    private Festival() {
        super(() -> new ProcessBuilder("festival", "--server").start());
    }

    public static void speak(String phrase) {
        try (AutoClosingProcess client = client()){
            client.writeToPipe(phrase);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static AutoClosingProcess client() throws IOException {
        return new AutoClosingProcess("festival_client", "--ttw", "--aucommand", "na_play $FILE");
    }


}
