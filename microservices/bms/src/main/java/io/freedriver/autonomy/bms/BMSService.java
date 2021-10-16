package io.freedriver.autonomy.bms;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class BMSService {

    private static final Path DEFAULT_SERIAL = Paths.get("/dev/serial/by-id/","usb-FTDI_FT232R_USB_UART_AB0LHNB0-if00-port0-");

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ConfigProperty(name = "ports.serial")
    Optional<String> serialPort;

    public Path getSerialPath() {
        return serialPort
                .map(Paths::get)
                .orElse(DEFAULT_SERIAL);
    }




}
