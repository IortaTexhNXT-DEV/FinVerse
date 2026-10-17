package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.report.core.CodeSetSource.CodeOption;
import java.util.List;
import java.util.function.Function;

/**
 * The value of a list parameter as the report header prints it: the name the user chose ("User
 * Access Approver"), not the code behind it ("UAM_APPROVER"). A value that is not in the list, or a
 * parameter that is not a list, prints as entered.
 */
final class LookupNames {

  private LookupNames() {}

  /**
   * The text printed for a parameter value.
   *
   * @param spec declared parameter
   * @param value validated value
   * @param options the options of a list source, by its key
   * @return the name of the chosen entry, or the value itself
   */
  static String shown(
      ParameterSpec spec, String value, Function<String, List<CodeOption>> options) {
    ParameterSpec offered = ParameterLookups.refine(spec);
    if (offered.type() != ParameterType.LOOKUP || offered.options().isEmpty()) {
      return value;
    }
    List<CodeOption> list;
    try {
      list = options.apply(offered.options().get(0));
    } catch (RuntimeException ex) {
      return value;
    }
    return list.stream()
        .filter(o -> value.equals(o.code()))
        .map(CodeOption::label)
        .findFirst()
        .orElse(value);
  }
}
