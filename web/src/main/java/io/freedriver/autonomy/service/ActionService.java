package io.freedriver.autonomy.service;

import io.freedriver.autonomy.entity.event.Event;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class ActionService<ST extends ActionService<ST>> {
    private static final Logger LOGGER = Logger.getLogger(ActionService.class.getName());

    public static final String LOG_ACTION = "log_action";

    protected abstract ST reference();
    protected abstract Class<ST> serviceKlazz();

    public final List<ActionMethod> getActionMethods() {
        return ActionMethod.ofActionService(reference(), serviceKlazz())
                .collect(Collectors.toList());
    }

    /**
     * Example Signature of an Action method.
     * @param event
     */
    @Action(LOG_ACTION)
    public void logAction(Event event) {
        LOGGER.log(Level.INFO, "ActionService "+LOG_ACTION, event);
    }
}
