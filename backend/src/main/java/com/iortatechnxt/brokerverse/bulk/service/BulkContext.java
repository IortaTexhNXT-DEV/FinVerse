package com.iortatechnxt.brokerverse.bulk.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Context of a bulk run.
 *
 * @param companyId company
 * @param jobNo upload number (reference for records created from it)
 * @param businessDate business date
 * @param parameters handler parameters chosen on screen (e.g. product), never null
 * @param maker user who uploaded the file (the maker of an approved upload), null when unknown
 */
public record BulkContext(
    Long companyId,
    String jobNo,
    LocalDate businessDate,
    Map<String, String> parameters,
    String maker) {

  /** Defensive copy. */
  public BulkContext {
    parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
  }

  /**
   * A context without the maker.
   *
   * @param companyId company
   * @param jobNo upload number
   * @param businessDate business date
   * @param parameters handler parameters
   */
  public BulkContext(
      Long companyId, String jobNo, LocalDate businessDate, Map<String, String> parameters) {
    this(companyId, jobNo, businessDate, parameters, null);
  }

  /**
   * A parameter.
   *
   * @param name name
   * @return value or null
   */
  public String parameter(String name) {
    return parameters.get(name);
  }
}
