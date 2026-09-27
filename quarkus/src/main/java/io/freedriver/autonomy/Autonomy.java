package io.freedriver.autonomy;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Autonomy {
    public static final String DEPLOYMENT = "autonomy";
    public static final String TEST_DEPLOYMENT = "autonomy-test";
    public static final Package PACKAGE = Autonomy.class.getPackage();

    public static void main(String... args) {
        System.out.println("Running main method");
        Quarkus.run(Application.class, Autonomy::exitHandler, args);
    }

    public static void exitHandler(Integer status, Throwable failure) {
        System.out.println("Exiting with status " + status);
        if (failure != null) {
            failure.printStackTrace();
        }
    }

    public static class Application implements QuarkusApplication {
        @Override
        public int run(String... args) throws Exception {
            Quarkus.waitForExit();
            return 0;
        }
    }
}
