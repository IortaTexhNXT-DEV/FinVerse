package com.iortatechnxt.finverse.report.core;

/** Renders a parameter value for the report header echo, e.g. a company id as its code and name. */
@FunctionalInterface
public interface ParameterDisplay {

  /** Shows every value as it was entered. */
  ParameterDisplay RAW = (spec, value) -> value;

  /**
   * Text shown for a value.
   *
   * @param spec parameter declaration
   * @param value validated value
   * @return display text
   */
  String display(ParameterSpec spec, String value);
}
