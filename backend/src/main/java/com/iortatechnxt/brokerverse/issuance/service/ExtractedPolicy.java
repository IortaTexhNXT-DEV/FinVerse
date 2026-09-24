package com.iortatechnxt.brokerverse.issuance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Policy data read from an e-policy (BRNB.104).
 *
 * @param policyNumbers policy numbers found, in document order (several for a multi-year account)
 * @param periodFrom period of insurance start
 * @param periodTo period of insurance end
 * @param premium total premium
 * @param arns Account Reference Numbers printed in the document
 * @param note what could not be read
 */
public record ExtractedPolicy(
    List<String> policyNumbers,
    LocalDate periodFrom,
    LocalDate periodTo,
    BigDecimal premium,
    List<String> arns,
    String note) {

  /** Nothing could be read. */
  public static final ExtractedPolicy NONE =
      new ExtractedPolicy(
          List.of(), null, null, null, List.of(), "No text could be read from the file");

  /** Defensive copies. */
  public ExtractedPolicy {
    policyNumbers = policyNumbers == null ? List.of() : List.copyOf(policyNumbers);
    arns = arns == null ? List.of() : List.copyOf(arns);
  }
}
