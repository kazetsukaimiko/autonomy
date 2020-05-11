package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import io.freedriver.autonomy.event.input.joystick.jstest.AllJoysticks;
import io.freedriver.autonomy.event.input.joystick.jstest.JSTestReader;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class AllJoysticksTest {

    private ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*4);

    private String version = "2.1.0";
    private String type =  "Joystick";
    private String name = "8BitDo SN30 Pro+";
    private List<String> axes = Arrays.asList("X, Y, Z, Rx, Ry, Rz, Hat0X, Hat0Y".split(", "));
    private List<String> buttons = Arrays.asList("BtnA, BtnB, BtnC, BtnX, BtnY, BtnZ, BtnTL, BtnTR, BtnTL2, BtnTR2".split(", "));

    private List<String> simulate(String version, String type, String name, List<String> axes, List<String> buttons) {
        Random r = new Random(System.currentTimeMillis());

        List<String> simulation = Stream.of(
                "Driver version is "+version+".",
                type+" ("+name+") has "+axes.size()+" axes ("+String.join(", ", axes)+")",
                "and "+buttons.size()+" buttons ("+String.join(", ", buttons)+").",
                "Testing ... (interrupt to exit)"
                ).collect(Collectors.toList());

        // Initial Values
        IntStream.range(0, axes.size())
                .mapToObj(idx -> simulatedEvent(JSTestEventType.AXIS_INITIAL, idx, 0))
                .forEach(simulation::add);
        IntStream.range(0, buttons.size())
                .mapToObj(idx -> simulatedEvent(JSTestEventType.BUTTON_INITIAL, idx, 0))
                .forEach(simulation::add);

        // Each normal values.
        IntStream.range(0, axes.size())
                .mapToObj(idx -> simulatedEvent(JSTestEventType.AXIS, idx, 65535))
                .forEach(simulation::add);
        IntStream.range(0, buttons.size())
                .mapToObj(idx -> simulatedEvent(JSTestEventType.BUTTON, idx, 1))
                .forEach(simulation::add);

        return simulation;
    }

    private static String simulatedEvent(JSTestEventType eventType, int idx, int value) {
        return "Event: type "+eventType.getTypeNumber()+", time -"+System.currentTimeMillis()+", number "+idx+", value "+value;
    }

    //@Test
    public void testJSTester() {
        //AllJoysticks aj = new AllJoysticks(pool, evt -> System.out.println(new JoystickEvent(Instant.now(), evt)));
        //aj.populate();
    }


    public Stream<String> simulate() {
        return simulate(version, type, name, axes, buttons).stream();
    }


    @Test
    public void simulationTest() {
        simulate().forEach(System.out::println);
        List<JSTestEvent> jsTestEvents = JSTestReader.readEvents(simulate())
                .collect(Collectors.toList());

        jsTestEvents.stream().map(JSTestEvent::getMetadata)
                .findFirst()
                .ifPresentOrElse(jsMetadata -> {
                    assertEquals(type, jsMetadata.getHardwareType(), "Type must match");
                    assertEquals(name, jsMetadata.getTitle(), "Title must match");
                    assertTrue(axes.stream().anyMatch(jsMetadata.getAxisNames()::containsValue), "Axis must be defined in metadata");
                    assertTrue(buttons.stream().anyMatch(jsMetadata.getButtonNames()::containsValue), "Button must be defined in metadata");
                }, () -> fail("JSMetadata must be present."));

        assertEquals(1, jsTestEvents.stream().map(JSTestEvent::getMetadata).distinct().count(),
                "All JSMetadata should point to same instance.");


        assertEquals(axes.size(), jsTestEvents.stream().filter(jsTestEvent -> jsTestEvent.getJsTestEventType() == JSTestEventType.AXIS_INITIAL).count());
        assertEquals(axes.size(), jsTestEvents.stream().filter(jsTestEvent -> jsTestEvent.getJsTestEventType() == JSTestEventType.AXIS).count());
        assertEquals(buttons.size(), jsTestEvents.stream().filter(jsTestEvent -> jsTestEvent.getJsTestEventType() == JSTestEventType.BUTTON_INITIAL).count());
        assertEquals(buttons.size(), jsTestEvents.stream().filter(jsTestEvent -> jsTestEvent.getJsTestEventType() == JSTestEventType.BUTTON).count());
    }

    @Test
    public void testAllJoysticks() throws Exception {
        List<JSTestEvent> events = new ArrayList<>();
        try (AllJoysticks aj = new AllJoysticks(pool, events::add, s -> s.limit(10))) {
            aj.waitForAllToClose();
        } catch (Exception e) {
            throw e;
        }
    }




}
