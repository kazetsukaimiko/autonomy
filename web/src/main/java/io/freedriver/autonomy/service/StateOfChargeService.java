package io.freedriver.autonomy.service;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.math.StateOfChargeConfig;
import io.freedriver.autonomy.rest.provider.ObjectMapperContextResolver;
import io.freedriver.util.file.DirectoryProviders;
import kaze.victron.VEDirectMessage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class StateOfChargeService {
    private static final Logger LOGGER = Logger.getLogger(StateOfChargeService.class.getName());

    private List<BigDecimal> charges = new ArrayList<>();
    private BigDecimal lastAvg;

    // TODO: Victron-Agnostic Bank Voltage
    public synchronized void actOnVEDirectMessage(@Observes @Default VEDirectMessage veDirectMessage) throws IOException {
        ensureEvent(veDirectMessage);
    }

    private void ensureEvent(VEDirectMessage veDirectMessage) {
        try {
            Path socPath = DirectoryProviders.CONFIG
                    .getProvider()
                    .subdir(Autonomy.DEPLOYMENT)
                    .file("soc_calculations.json")
                    .get();

            if (!Files.exists(socPath)) {
                ObjectMapperContextResolver.getMapper().writeValue(socPath.toFile(), new StateOfChargeConfig());
            }

            StateOfChargeConfig socConfig = ObjectMapperContextResolver.getMapper().readValue(socPath.toFile(), StateOfChargeConfig.class);

            reportAverage(veDirectMessage, socConfig.calculate(veDirectMessage.getMainVoltage()));

        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Couldn't calculate SOC: ", ioe);
        }
    }

    private synchronized void reportAverage(VEDirectMessage veDM, BigDecimal bigDecimal) {
        charges.add(0, bigDecimal.setScale(2, RoundingMode.HALF_UP));
        if (charges.size() > 1000) {
            charges = charges.subList(0, 1000);
        }
        BigDecimal newAvg = charges.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(charges.size()), 2, RoundingMode.HALF_UP);
        if (!Objects.equals(lastAvg, newAvg)) {
            lastAvg = newAvg;
            LOGGER.info(
                    veDM.getProductType().getProductName() + " ("+veDM.getSerialNumber()+") Main Voltage SOC: "
                    + bigDecimal.setScale(2, RoundingMode.FLOOR).toPlainString() + "%");
        }
    }
}
