package io.freedriver.autonomy;

import io.freedriver.autonomy.jpa.converter.FirmwareVersionConverter;
import io.freedriver.autonomy.jpa.entity.EntityBase;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.vedirect.VEDirectMessageService;
import kaze.math.measurement.units.Current;
import kaze.math.measurement.units.Energy;
import kaze.math.measurement.units.Potential;
import kaze.math.measurement.units.Power;
import kaze.math.number.ScaledNumber;
import kaze.victron.FirmwareVersion;
import kaze.victron.LoadOutputState;
import kaze.victron.RelayState;
import kaze.victron.StateOfOperation;
import kaze.victron.TrackerOperation;
import kaze.victron.VictronDevice;
import kaze.victron.VictronProduct;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(Arquillian.class)
//@DefaultDeployment
public class VEDirectMessageServiceTest extends BaseITTest {
    private static final Random R = new Random(System.currentTimeMillis());
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private double pvPower = 500;
    private double pvYield = 0d;
    private double mainVoltage = 48;
    private Instant lastMessage = Instant.now();

    @Inject
    VEDirectMessageService messageService;

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(VEDirectMessageService.class)

                .addPackages(false, Autonomy.PACKAGE)
                .addAsResource("project-defaults.yml") // TODO Test yaml with memory only
                .addAsResource("META-INF/persistence.xml") // TODO Test xml -> drop and create. Prod -> create.
                .addPackages(true, FirmwareVersionConverter.class.getPackage())
                .addPackages(true, VEDirectMessageService.class.getPackage())
                .addPackages(true, "kaze.math")
                .addPackages(true, "kaze.victron")
                .addPackages(true, "io.freedriver.autonomy.jpa")
                .addPackages(true, EntityBase.class.getPackage())
                ;
    }


    @Test
    public void testBasicSave() {
        VictronDevice device = new VictronDevice();
        device.setSerialNumber("HQXABCBBQ");
        device.setType(VictronProduct.BLUESOLAR_MPPT_150_100);


        List<VEDirectMessage> fromEmpty = messageService.byDevice(device)
                .collect(Collectors.toList());

        assertTrue(fromEmpty.isEmpty());
        assertEquals(0L, messageService.countByDevice(device));


        // Make 1 minute's worth of data
        List<kaze.victron.VEDirectMessage> messages = makeMessages(device, 60)
                .collect(Collectors.toList());

        List<VEDirectMessage> jpaEntities = messages.stream()
                .map(messageService::save)
                .collect(Collectors.toList());

        assertEquals(messages.size(), messageService.countByDevice(device));

        assertEquals(messages.size(), jpaEntities.size());

        List<VEDirectMessage> fromService = messageService.byDevice(device)
                .collect(Collectors.toList());


        assertEquals(messages.size(), fromService.size());

        //VEDirectMessage fromService = message
    }

    public Stream<kaze.victron.VEDirectMessage> makeMessages(VictronDevice device, int count) {
        Instant beginning = Instant.now().minus(Duration.ofSeconds(count));
        return IntStream.range(0, count)
                .mapToObj(seconds -> makeMessage(device, beginning.plus(Duration.ofSeconds(seconds))));
    }

    public kaze.victron.VEDirectMessage makeMessage(VictronDevice device, Instant forInstant) {
        kaze.victron.VEDirectMessage message = new kaze.victron.VEDirectMessage();

        message.setTimestamp(forInstant);

        message.setSerialNumber(device.getSerialNumber());
        message.setFirmwareVersion(new FirmwareVersion("v1.50"));

        // ENUMS

        message.setLoadOutputState(LoadOutputState.OFF);
        message.setProductType(device.getType());
        message.setStateOfOperation(StateOfOperation.BULK);
        message.setTrackerOperation(TrackerOperation.MPP_TRACKER_ACTIVE);
        message.setRelayState(RelayState.ON);

        message.setErrorCode(null);
        message.setOffReason(null);

        // MEASUREMENTS



        message.setMainVoltage(new Potential(ScaledNumber.of(mainVoltage)));
        message.setMainCurrent(new Current(ScaledNumber.of(pvPower/mainVoltage)));
        message.setPanelPower(new Power(ScaledNumber.of(pvPower)));

        pvYield = pvYield + tokWhMultiplier(pvPower, forInstant);

        message.setPanelVoltage(new Potential(ScaledNumber.of(96)));
        message.setMaxPowerToday(new Power(ScaledNumber.of(pvPower)));
        message.setMaxPowerYesterday(new Power(ScaledNumber.of(pvPower)));

        message.setYieldToday(new Energy(ScaledNumber.of(pvYield)));
        message.setYieldYesterday(new Energy(ScaledNumber.of(pvPower*8)));
        message.setResettableYield(new Energy(ScaledNumber.of(1500000)));

        return message;
    }

    private double tokWhMultiplier(double pvPower, Instant forInstant) {
        Duration between = Duration.between(forInstant, lastMessage);
        lastMessage = forInstant;
        return ((double) between.toMillis())/((double) ONE_HOUR.toMillis()) * pvPower;
    }

}
