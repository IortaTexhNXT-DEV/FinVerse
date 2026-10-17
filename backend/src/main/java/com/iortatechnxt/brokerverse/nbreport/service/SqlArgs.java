package com.iortatechnxt.brokerverse.nbreport.service;

import java.util.HashMap;
import java.util.Map;

/** Fluent named bind parameters that accept null values (optional report filters). */
public final class SqlArgs {

  private final Map<String, Object> values = new HashMap<>();

  private SqlArgs() {}

  /**
   * Starts the parameters with the company.
   *
   * @param companyId company
   * @return parameters
   */
  public static SqlArgs company(long companyId) {
    return new SqlArgs().with("company", companyId);
  }

  /**
   * Adds a parameter.
   *
   * @param name name
   * @param value value, null allowed
   * @return this
   */
  public SqlArgs with(String name, Object value) {
    values.put(name, value);
    return this;
  }

  /**
   * The parameters.
   *
   * @return name to value
   */
  public Map<String, Object> map() {
    return values;
  }
}
