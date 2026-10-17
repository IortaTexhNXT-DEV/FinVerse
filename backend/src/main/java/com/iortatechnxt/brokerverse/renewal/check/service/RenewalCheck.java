package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;

/**
 * One sanitation, matching or eligibility check of a renewal (BRRN.020; RENEWAL_DESIGN section
 * 8.1). Each check is a bean; its activity and the severity of a failure are the check settings
 * (maker-checker data), so the rules are data and the decisions are explained.
 */
public interface RenewalCheck {

  /**
   * Code of the check (row of {@code rnw_check_setting}).
   *
   * @return code, e.g. {@code OUTSTANDING_PREMIUM}
   */
  String code();

  /**
   * Evaluates the check on a renewal.
   *
   * @param context the renewal and its lazily loaded facts
   * @return outcome, message and detail
   */
  Verdict evaluate(CheckContext context);

  /**
   * The result of a check.
   *
   * @param outcome outcome
   * @param message message shown on the Checks and Bucket tab
   * @param detail detail for the report (compared values), may be null
   */
  record Verdict(CheckOutcome outcome, String message, String detail) {

    /**
     * A pass.
     *
     * @param message message
     * @return verdict
     */
    public static Verdict pass(String message) {
      return new Verdict(CheckOutcome.PASS, message, null);
    }

    /**
     * A failure.
     *
     * @param message message
     * @param detail detail
     * @return verdict
     */
    public static Verdict fail(String message, String detail) {
      return new Verdict(CheckOutcome.FAIL, message, detail);
    }

    /**
     * A warning.
     *
     * @param message message
     * @return verdict
     */
    public static Verdict warn(String message) {
      return new Verdict(CheckOutcome.WARN, message, null);
    }

    /**
     * Information only.
     *
     * @param message message
     * @return verdict
     */
    public static Verdict info(String message) {
      return new Verdict(CheckOutcome.INFO, message, null);
    }

    /**
     * Not applicable.
     *
     * @param message message
     * @return verdict
     */
    public static Verdict notApplicable(String message) {
      return new Verdict(CheckOutcome.NOT_APPLICABLE, message, null);
    }
  }
}
