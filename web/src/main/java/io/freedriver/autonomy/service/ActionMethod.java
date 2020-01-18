package io.freedriver.autonomy.service;

import io.freedriver.autonomy.entity.event.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface ActionMethod extends Consumer<Event> {
    ActionService<?> service();
    Class<? extends ActionService<?>> serviceKlazz();
    Class<? extends Event> eventKlazz();
    String actionName();

    static <XST extends ActionService<XST>> Stream<ActionMethod> ofActionService(XST service, Class<XST> serviceKlazz) {
        return Stream.of(serviceKlazz.getMethods())
                .map(method -> ActionMethod.ofMethod(service, serviceKlazz, method))
                .flatMap(Optional::stream);
    }

    @SuppressWarnings("unchecked")
    static <XST extends ActionService<XST>> Optional<ActionMethod> ofMethod(XST service, Class<XST> serviceKlazz, Method method) {
        if (method.isAnnotationPresent(Action.class)) {
            String actionName = method.getAnnotation(Action.class).value();
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0].isAssignableFrom(Event.class)) {
                return Optional.of(new ActionMethod() {
                    @Override
                    public ActionService<?> service() {
                        return service;
                    }

                    @Override
                    public Class<? extends ActionService<?>> serviceKlazz() {
                        return serviceKlazz;
                    }

                    @Override
                    public Class<? extends Event> eventKlazz() {
                        return (Class<? extends Event>) method.getParameterTypes()[0];
                    }

                    @Override
                    public String actionName() {
                        return actionName;
                    }

                    @Override
                    public void accept(Event event) {
                        try {
                            method.invoke(service, event);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new ActionInvocationException(e);
                        }
                    }
                });
            }
        }
        return Optional.empty();
    }
}
