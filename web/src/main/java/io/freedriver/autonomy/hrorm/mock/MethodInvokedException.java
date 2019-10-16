package io.freedriver.autonomy.hrorm.mock;

import java.lang.reflect.Method;

public class MethodInvokedException extends RuntimeException {
    private final Method method;

    public MethodInvokedException(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }
}
