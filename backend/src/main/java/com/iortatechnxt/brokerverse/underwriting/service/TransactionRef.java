package com.iortatechnxt.brokerverse.underwriting.service;

/**
 * Identifies one premium transaction of a policy: the original issue ({@code endorsementNo} 0) or
 * one of its endorsements (1, 2...). Used as the key between underwriting and the reinsurance
 * module; it corresponds to the accounting source reference {@code POLICY:<policyId>} or {@code
 * POLICY:<policyId>:ENDT:<endorsementNo>}.
 *
 * @param policyId policy id
 * @param endorsementNo 0 for the original issue, else the endorsement number
 */
public record TransactionRef(Long policyId, int endorsementNo) {

  /**
   * Reference of an original issue.
   *
   * @param policyId policy id
   * @return reference
   */
  public static TransactionRef original(Long policyId) {
    return new TransactionRef(policyId, 0);
  }

  /**
   * Whether this is the original issue.
   *
   * @return true for endorsement number 0
   */
  public boolean isOriginal() {
    return endorsementNo == 0;
  }
}
