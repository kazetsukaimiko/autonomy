package io.freedriver.autonomy.util;

import java.util.concurrent.Callable;

public class AutoClosingProcess extends ProcessSpawner implements AutoCloseable {

    public AutoClosingProcess(String... command) {
        this(() -> new ProcessBuilder(command).start());
    }

    protected AutoClosingProcess(Callable<Process> supplier) {
        super(supplier);
    }


}
