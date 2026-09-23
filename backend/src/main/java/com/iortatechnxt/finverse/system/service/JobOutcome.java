package com.iortatechnxt.finverse.system.service;

/**
 * Result of a successful job execution.
 *
 * @param itemsProcessed number of items created or processed
 * @param message summary shown in the job monitor
 */
public record JobOutcome(int itemsProcessed, String message) {}
