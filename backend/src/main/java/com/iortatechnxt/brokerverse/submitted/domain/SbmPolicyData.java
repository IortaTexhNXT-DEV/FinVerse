package com.iortatechnxt.brokerverse.submitted.domain;

/**
 * The maintainable data of a masterlist record: what a source row, a confirmed extraction, a manual
 * entry or a migrated row provides (BRIDSP-01-04, 33).
 *
 * @param segment segment code (LOV SBM_SEGMENT)
 * @param businessType NB or RB
 * @param loan bank loan
 * @param assured assured and contacts
 * @param terms insurer, policy and period
 * @param risk risk details
 * @param marks lists the policy is on
 */
public record SbmPolicyData(
    String segment,
    SbmBusinessType businessType,
    SbmLoan loan,
    SbmAssured assured,
    SbmTerms terms,
    SbmRisk risk,
    SbmMarks marks) {

  /** Empty groups become their NONE value. */
  public SbmPolicyData {
    loan = loan == null ? SbmLoan.NONE : loan;
    risk = risk == null ? SbmRisk.NONE : risk;
    marks = marks == null ? SbmMarks.NONE : marks;
  }

  /**
   * The same data with terms in a currency when they carry none.
   *
   * @param code currency (the base currency of the company)
   * @return data
   */
  public SbmPolicyData orInCurrency(String code) {
    if (terms == null || terms.currency() != null) {
      return this;
    }
    return new SbmPolicyData(
        segment, businessType, loan, assured, terms.orInCurrency(code), risk, marks);
  }
}
