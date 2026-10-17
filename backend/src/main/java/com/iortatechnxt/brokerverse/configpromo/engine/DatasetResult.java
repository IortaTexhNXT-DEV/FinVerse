package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * What an import did to one dataset.
 *
 * @param code dataset code
 * @param inserted items added
 * @param updated items updated
 * @param deactivated items of the target deactivated
 * @param removed items removed from replaced collections
 */
public record DatasetResult(String code, int inserted, int updated, int deactivated, int removed) {}
