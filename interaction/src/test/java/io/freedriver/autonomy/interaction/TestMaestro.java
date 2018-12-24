package io.freedriver.autonomy.interaction;


import com.jamief.maestro.binding.LibUsbDriverBinding;
import com.jamierf.maestro.MaestroServoController;
import com.jamierf.maestro.api.Product;
import com.jamierf.maestro.binding.DriverBinding;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.usb4java.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TestMaestro {

    /*
    private Context context;

    @BeforeAll
    public void init() {
        context = new Context();
        int result = LibUsb.init(context);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to initialize libusb: ", result);
    }

    @AfterAll
    public void close() {
        LibUsb.exit(context);
    }
    */


    @Test
    public void testServos() {

        Context context = null;
        Product product = Product.MICRO6;
        final DeviceHandle handle = LibUsb.openDeviceWithVidPid(context, (short)product.getVendorId(), (short)product.getProductId());
        DriverBinding driverBinding = new LibUsbDriverBinding(handle);
        MaestroServoController maestroServoController = new MaestroServoController(driverBinding);

        maestroServoController.getStatus().forEach(System.out::println);



    }

    protected void process(int result) {
        if (result < LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to perform operation: ", result);
        }
    }


}
