package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * A changed field of an item.
 *
 * @param column column
 * @param from value in the target
 * @param to value in the package
 */
public record FieldChange(String column, Object from, Object to) {}
