package io.freedriver.autonomy.interaction;

import org.junit.jupiter.api.Test;

import javax.usb.UsbException;
import java.io.IOException;
import java.nio.file.Paths;

public class TestMaestroInteraction {

    @Test
    public void basic() throws UsbException, IOException {
        MaestroInteraction maestroInteraction = new MaestroInteraction(Product.MICRO6);
//org.usb4java.javax.S

        for (int i=1;i<5;i++) {
            System.out.println("Getting servo position " + i);
            System.out.println(maestroInteraction.getServoPosition(i));
        }

    }

    @Test
    public void testSimple() throws IOException {
        TTYInteraction ttyInteraction = new TTYInteraction(Paths.get("/dev/serial/by-id/usb-Pololu_Corporation_Pololu_Micro_Maestro_6-Servo_Controller_00049501-if00"));
        System.out.println(ttyInteraction.getServoValue(1));
    }

    @Test
    public void testLib() {
        LibInteraction li = new LibInteraction(com.jamierf.maestro.api.Product.MICRO6);
    }
}
