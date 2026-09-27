package com.iortatechnxt.brokerverse.report.core;

import java.util.List;

/**
 * Port of the report platform: the codes offered by a {@link ParameterType#CODE_SET} parameter (for
 * example the risk codes or the sales units of a Renewal report). Implemented by the business
 * modules; the report API lists the options of a source by its key.
 */
public interface CodeSetSource {

  /**
   * Key of the source, named in {@link ParameterSpec#codeSet(String, String, String)}.
   *
   * @return key, e.g. {@code renewal.riskCode}
   */
  String source();

  /**
   * The codes and labels offered for a company.
   *
   * @param companyId company
   * @return options in display order
   */
  List<CodeOption> options(Long companyId);

  /**
   * One option.
   *
   * @param code code sent in the parameter value
   * @param label label shown to the user
   */
  record CodeOption(String code, String label) {}
}
