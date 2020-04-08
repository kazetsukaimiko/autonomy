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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class StateOfChargeService {
    private static final Logger LOGGER = Logger.getLogger(StateOfChargeService.class.getName());

    private BigDecimal lastSOC;

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

            BigDecimal bigDecimal = socConfig.calculate(veDirectMessage.getMainVoltage());
            if (notEquals(bigDecimal)) {
                LOGGER.info("New SOC: " + bigDecimal.setScale(2, RoundingMode.FLOOR).toPlainString() + "%");
            }

        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Couldn't calculate SOC: ", ioe);
        }
    }

    private boolean notEquals(BigDecimal bigDecimal) {
        if (lastSOC == null) {
            lastSOC = bigDecimal;
            return true;
        }
        int newScale = Math.min(
                2,
                Math.max(bigDecimal.scale(), lastSOC.scale()));
        if (!Objects.equals(
                bigDecimal.setScale(newScale, RoundingMode.FLOOR),
                lastSOC.setScale(newScale, RoundingMode.FLOOR))) {
            lastSOC = bigDecimal.stripTrailingZeros();
            return true;
        }
        return false;
    }
}
