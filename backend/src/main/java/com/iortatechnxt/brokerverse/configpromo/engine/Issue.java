package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * A finding of the dry run: a blocker (the import cannot be approved) or a warning.
 *
 * @param dataset dataset code, null for the whole package
 * @param keyText natural key of the item, null for the whole dataset
 * @param message message in business words
 */
public record Issue(String dataset, String keyText, String message) {}
