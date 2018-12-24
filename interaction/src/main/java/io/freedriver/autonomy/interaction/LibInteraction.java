package io.freedriver.autonomy.interaction;

import com.jamief.maestro.binding.LibUsbDriverBinding;
import com.jamierf.maestro.MaestroServoController;
import com.jamierf.maestro.api.Product;
import com.jamierf.maestro.binding.DriverBinding;
import org.usb4java.Context;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;

public class LibInteraction {
    private final Context context;
    private final MaestroServoController maestroServoController;

    public LibInteraction(Product product) {
        context = new Context();


        final DeviceHandle handle = ofProduct(product);

        DriverBinding driverBinding = new LibUsbDriverBinding(handle);
        maestroServoController = new MaestroServoController(driverBinding);

        maestroServoController.getStatus().forEach(System.out::println);


    }

    public DeviceHandle ofProduct(Product product) {
        return LibUsb.openDeviceWithVidPid(context, (short)product.getVendorId(), (short)product.getProductId());
    }
}
