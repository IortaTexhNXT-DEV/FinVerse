package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * A column holding the surrogate id of a row of another dataset (or of its own): it is exported as
 * the natural key of that row and remapped to the id of the same row in the target.
 *
 * @param column referencing column
 * @param dataset code of the referenced dataset
 */
public record Reference(String column, String dataset) {}
