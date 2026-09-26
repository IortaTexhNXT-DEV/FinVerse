package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Periodic KYC review cycle (BRNB.110): high-risk clients are reviewed every {@value
 * #HIGH_RISK_MONTHS} months, others every {@value #STANDARD_MONTHS} months (system parameters). The
 * frequency per risk rating is a BDOI decision still open (Q21), hence configurable.
 */
@Component
public class KycReviewPolicy {

  /** Parameter: months between reviews of standard and low risk clients. */
  public static final String STANDARD_MONTHS = "KYC_REVIEW_MONTHS";

  /** Parameter: months between reviews of high risk clients. */
  public static final String HIGH_RISK_MONTHS = "KYC_REVIEW_MONTHS_HIGH_RISK";

  /** Parameter: days ahead in which a review is listed as coming due. */
  public static final String DUE_WINDOW_DAYS = "KYC_DUE_WINDOW_DAYS";

  /** Risk rating with the short review cycle. */
  public static final String HIGH_RISK = "HIGH";

  private static final int DEFAULT_STANDARD = 36;
  private static final int DEFAULT_HIGH_RISK = 12;
  private static final int DEFAULT_WINDOW = 30;

  private final SystemParameterService parameters;

  /**
   * Creates the policy.
   *
   * @param parameters system parameters
   */
  public KycReviewPolicy(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Next review date after a verification.
   *
   * @param riskRating risk rating code (may be null = standard)
   * @param verifiedOn verification date
   * @return review due date
   */
  public LocalDate nextReview(String riskRating, LocalDate verifiedOn) {
    int months =
        HIGH_RISK.equals(riskRating)
            ? parameters.intValue(HIGH_RISK_MONTHS, DEFAULT_HIGH_RISK)
            : parameters.intValue(STANDARD_MONTHS, DEFAULT_STANDARD);
    return verifiedOn.plusMonths(months);
  }

  /**
   * Last date of the "coming due" window.
   *
   * @param today business date
   * @return today plus the configured window
   */
  public LocalDate dueHorizon(LocalDate today) {
    return today.plusDays(parameters.intValue(DUE_WINDOW_DAYS, DEFAULT_WINDOW));
  }
}
