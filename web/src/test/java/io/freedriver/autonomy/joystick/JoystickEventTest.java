package io.freedriver.autonomy.joystick;

import io.freedriver.autonomy.event.input.joystick.jstest.AllJoysticks;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JoystickEventTest {

    ExecutorService single = Executors.newSingleThreadExecutor();

    /*
    private JoystickEvent generate(JoystickEventType eventType, int idx) {
        JoystickEvent joystickEvent = new JoystickEvent();
        joystickEvent.setJoystickEventType(eventType);
        joystickEvent.setNumber(1);
        joystickEvent.setValue(eventType.getValue());

    }
     */

    //@Test
    public void testJoystickEvent() throws IOException, InterruptedException {
        //JSTestReader jsTestReader = new JSTestReader(, single, this::testEvent);

        ExecutorService pool = Executors.newFixedThreadPool(16);
        AllJoysticks aj = new AllJoysticks(pool, msg -> {
            System.out.println(msg);
        });

        aj.poll();
    }

    private void testEvent(JSTestEvent jsTestEvent) {
        testEvent(new JoystickEvent(Instant.now().toEpochMilli(), jsTestEvent));
    }

    private void testEvent(JoystickEvent joystickEvent) {
        System.out.println(joystickEvent);


    }
}
