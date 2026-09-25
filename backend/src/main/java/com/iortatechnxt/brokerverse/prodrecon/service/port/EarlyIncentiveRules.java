package com.iortatechnxt.brokerverse.prodrecon.service.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Seam of the early remittance incentive rule (PRCID.028, RMTID.023, OQ23): the rate and window
 * that apply to a booked invoice, e.g. 2% of the premium of CLG / CBG motor and fire accounts
 * remitted within 30 days of inception. The rule belongs to remittance ({@code
 * rem_incentive_rule}); until it is connected (a remittance bean implementing this port, or a port
 * moved to {@code opsledger} in the integration wave) the default adapter knows no rule and the
 * validation reports every line as "rule pending".
 */
public interface EarlyIncentiveRules {

  /**
   * The terms that apply to an invoice.
   *
   * @param companyId company
   * @param subject insurer, classification and dates of the invoice
   * @return terms, empty when no rule applies (or none is known yet)
   */
  Optional<Terms> termsFor(Long companyId, Subject subject);

  /** Date the window starts from. */
  enum Basis {
    /** Policy inception. */
    INCEPTION,
    /** Booking date. */
    BOOKING
  }

  /**
   * What a rule is looked up by.
   *
   * @param insurerCode insurer
   * @param segment market segment
   * @param productLine product line
   * @param inceptionDate inception
   * @param bookingDate booking date
   */
  record Subject(
      String insurerCode,
      String segment,
      String productLine,
      LocalDate inceptionDate,
      LocalDate bookingDate) {}

  /**
   * A rule's terms.
   *
   * @param ratePercent incentive rate in percent of the basic premium
   * @param windowDays days allowed between the basis date and the remittance
   * @param basis inception or booking date
   * @param source reference of the rule (shown on the report)
   */
  record Terms(BigDecimal ratePercent, int windowDays, Basis basis, String source) {}
}
