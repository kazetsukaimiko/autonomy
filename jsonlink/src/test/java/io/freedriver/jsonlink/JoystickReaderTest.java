package io.freedriver.jsonlink;

import io.freedriver.controller.AllJoysticks;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

@Disabled
public class JoystickReaderTest {

    //@Test
    public void testJoysticks() {
        //AllJoysticks aj = new AllJoysticks(System.out::println);
        //aj.populate();
/*
        List<JoystickReader> readers = new ArrayList<>();
        for (JoystickReader reader : JoystickReader.getJoysticks()) {
            System.out.println(reader.getPath());
            while (true) {
                System.out.println(reader.fetch());
            }
        }
*/

        /*
        Process process = new ProcessBuilder(
                "jstest",
                "--event",
                "/dev/input/js1")
                .start();
        ProcessUtil.linesInputStream(process.getInputStream())
                .filter(JoystickEvent::validEvent)
                .map(joystickEventString -> new JoystickEvent(Paths.get("/dev/input/js1"), joystickEventString))
                .forEach(System.out::println);
*/


    }
}
