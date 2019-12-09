package io.freedriver.autonomy.util;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

public abstract class ProcessSpawner implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ProcessSpawner.class.getName());
    private final Callable<Process> supplier;
    private Process process;

    protected ProcessSpawner(Callable<Process> supplier) {
        this.supplier = supplier;
        getProcess();
    }

    public Process getProcess() {
        if (process == null || !process.isAlive()) {
            try {
                LOGGER.log(FINE, "Spawning process");
                process = supplier.call();
            } catch (Exception e) {
                throw new RuntimeException("Couldn't spawn process", e);
            }
        } else {
            LOGGER.log(FINE, "Process already alive");
        }
        return process;
    }

    protected void writeToPipe(String phrase) throws IOException {
        getProcess().getOutputStream().write(phrase.getBytes());
    }

    @Override
    public void close() throws Exception {
//dg getProcess()                     f3ew2csd
    }
}
