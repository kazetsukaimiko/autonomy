package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import io.freedriver.autonomy.entity.event.input.joystick.JoystickEvent;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JSTestTester {

    private ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*4);

    //@Test
    public void testJSTester() {
        AllJoysticks aj = new AllJoysticks(pool, evt -> System.out.println(new JoystickEvent(Instant.now(), evt)));
        aj.populate();
    }

}
