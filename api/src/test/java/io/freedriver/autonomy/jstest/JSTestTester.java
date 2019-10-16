package io.freedriver.autonomy.jstest;

import io.freedriver.autonomy.entity.JoystickEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JSTestTester {

    private ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*4);

    //@Test
    public void testJSTester() {
        AllJoysticks aj = new AllJoysticks(pool, evt -> System.out.println(new JoystickEvent(evt)));
        aj.populate();
    }

}
