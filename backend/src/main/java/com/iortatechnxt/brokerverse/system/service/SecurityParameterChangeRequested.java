package com.iortatechnxt.brokerverse.system.service;

/**
 * Published when a change of a security parameter waits for a second approval.
 *
 * @param key parameter
 * @param description what the parameter does
 * @param currentValue value in force
 * @param requestedValue value asked for
 * @param requestedBy user who asked for the change
 */
public record SecurityParameterChangeRequested(
    String key,
    String description,
    String currentValue,
    String requestedValue,
    String requestedBy) {}
