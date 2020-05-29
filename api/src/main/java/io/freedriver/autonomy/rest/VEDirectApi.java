package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import kaze.victron.VEDirectColumn;
import kaze.victron.VictronDevice;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path(VEDirectApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface VEDirectApi {
    String ROOT = "/vedirect";
    String DEVICE_PATH = "/device";
    String DEVICE_SERIAL = "deviceSerial";
    String DEVICE_SERIAL_PATH = DEVICE_PATH + "/{"+DEVICE_SERIAL+"}";
    String NUMBER = "number";
    String UNITS = "units";
    String MESSAGE_TIME_PATH = DEVICE_SERIAL_PATH + "/last/{"+NUMBER+"}/{"+UNITS+"}";
    String COLUMN = "column";
    String COLUMN_DATA_PATH = MESSAGE_TIME_PATH + "/{"+ COLUMN +"}";

    @GET
    @Path(DEVICE_PATH)
    Set<VictronDevice> getDevices();

    @GET
    @Path(DEVICE_SERIAL_PATH)
    List<VEDirectMessage> getDevices(@PathParam(DEVICE_SERIAL) String serial);

    @GET
    @Path(MESSAGE_TIME_PATH)
    List<VEDirectMessage> getDevicesFrom(@PathParam(DEVICE_SERIAL) String serial, @PathParam(NUMBER) Integer number, @PathParam(UNITS) ChronoUnit chronoUnit);

    @GET
    @Path(COLUMN_DATA_PATH)
    <T> Map<T, Integer> getColumnData(@PathParam(DEVICE_SERIAL) String serial, @PathParam(NUMBER) Integer number, @PathParam(UNITS) ChronoUnit chronoUnit, @PathParam(COLUMN) VEDirectColumn column);
}
