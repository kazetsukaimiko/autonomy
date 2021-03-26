package io.freedriver.autonomy;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.h2.tools.Server;

import java.sql.SQLException;
import java.util.function.BiConsumer;

@QuarkusMain
public class Autonomy {
    public static final String DEPLOYMENT = "autonomy";
    public static final String TEST_DEPLOYMENT = "autonomy-test";
    public static final Package PACKAGE = Autonomy.class.getPackage();

    public static void main(String ... args) throws SQLException {
              System.out.println("Running main method");
        Server server = org.h2.tools.Server.createTcpServer("-tcpShutdown", "tcp://localhost:9092", "-tcpPassword", "sa");
        server.start();
        Quarkus.run(Application.class, Autonomy.exitHandler(server), args);
    }

    public static BiConsumer<Integer, Throwable> exitHandler(final Server s) {
        return (i, t) -> Autonomy.exitHandler(i, t, s);
    }

    public static void exitHandler(Integer i, Throwable t, Server s) {
        System.out.println("Exiting with status " + i);
        if (t != null) {
            t.printStackTrace();
        }
        System.out.println("Stopping H2");
        s.stop();
    }

    public static class Application implements QuarkusApplication {
        @Override
        public int run(String... args) throws Exception {
            System.out.println("H2 Server started");
            Quarkus.waitForExit();
            return 0;
        }
    }
}
