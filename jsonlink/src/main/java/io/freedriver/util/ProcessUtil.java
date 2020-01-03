package io.freedriver.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Stream;

public class ProcessUtil {
    private ProcessUtil() {
        // Prevent construction
    }

    public static Stream<String> linesInputStream(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream)).lines();
    }

    /*
    public static String readFromInputStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString().trim();
    }

     */
}
