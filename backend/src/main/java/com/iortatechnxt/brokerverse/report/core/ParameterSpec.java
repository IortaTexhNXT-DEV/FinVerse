package com.iortatechnxt.brokerverse.report.core;

import java.util.List;

/**
 * Declares one report parameter.
 *
 * @param name key used in requests
 * @param label UI label
 * @param type input type
 * @param required mandatory flag
 * @param options allowed values for SELECT
 * @param defaultValue default value (ISO date keywords TODAY, MONTH_START, YEAR_START allowed)
 */
public record ParameterSpec(
    String name,
    String label,
    ParameterType type,
    boolean required,
    List<String> options,
    String defaultValue) {

  /** Canonical constructor normalising options. */
  public ParameterSpec {
    options = options == null ? List.of() : List.copyOf(options);
  }

  /**
   * Mandatory parameter.
   *
   * @param name name
   * @param label label
   * @param type type
   * @return spec
   */
  public static ParameterSpec required(String name, String label, ParameterType type) {
    return new ParameterSpec(name, label, type, true, List.of(), null);
  }

  /**
   * Optional parameter.
   *
   * @param name name
   * @param label label
   * @param type type
   * @return spec
   */
  public static ParameterSpec optional(String name, String label, ParameterType type) {
    return new ParameterSpec(name, label, type, false, List.of(), null);
  }

  /**
   * Mandatory SELECT parameter with a default.
   *
   * @param name name
   * @param label label
   * @param options options
   * @param defaultValue default
   * @return spec
   */
  public static ParameterSpec select(
      String name, String label, List<String> options, String defaultValue) {
    return new ParameterSpec(name, label, ParameterType.SELECT, true, options, defaultValue);
  }

  /**
   * Returns a copy with a default value.
   *
   * @param value default
   * @return spec
   */
  public ParameterSpec withDefault(String value) {
    return new ParameterSpec(name, label, type, required, options, value);
  }
}
