package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * A column of a table as the database describes it.
 *
 * @param name column name
 * @param type PostgreSQL type name (udt_name, e.g. varchar, numeric, timestamptz)
 * @param nullable whether the column accepts null
 * @param hasDefault whether the column has a default value or is an identity column
 */
public record ColumnInfo(String name, String type, boolean nullable, boolean hasDefault) {}
