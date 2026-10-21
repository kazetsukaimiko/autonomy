package io.freedriver.autonomy.service.startup;

import java.util.function.BiConsumer;

import io.freedriver.jsonlink.config.v2.MappingsAction;

/**
 * Startup step. {@link MappingsAction} is an interface, so this implements it.
 * The runner calls {@link #accept} under the board lock, except a delay.
 */
public abstract class AutonomyMappingsAction implements MappingsAction, BiConsumer<GpioControl, ApplianceControl> {}
