package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSetting;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSettingRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * {@code TSI_THRESHOLD} (Annex BRRN.020 SC-12, SC-13; R23-01, R23-02, R24-02; FR-RN-020): a total
 * sum insured above the threshold goes to TSU or proposal handling. The threshold is the parameter
 * of the check setting (default PHP 250,000,000.00); the failure has the severity of the setting
 * (Review) and the decision matrix proposes For Proposal.
 */
@Component
public class TsiThresholdCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "TSI_THRESHOLD";

  /** Detail of a TSI above the threshold under BDOI's rule (Clean, For Quotation). */
  public static final String QUOTATION_DETAIL = "quotation";

  /** Default threshold. */
  static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("250000000");

  private final CheckSettingRepository settings;
  private final SystemParameterService parameters;

  /**
   * Creates the check.
   *
   * @param settings check settings (threshold)
   * @param parameters system parameters (route of a TSI above the threshold)
   */
  public TsiThresholdCheck(CheckSettingRepository settings, SystemParameterService parameters) {
    this.settings = settings;
    this.parameters = parameters;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    SnapshotPremium premium = context.candidate().getSnapshot().premium();
    BigDecimal tsi = premium == null ? null : premium.totalSumInsured();
    if (tsi == null) {
      return Verdict.notApplicable("No total sum insured");
    }
    BigDecimal threshold = threshold();
    if (tsi.compareTo(threshold) > 0 && quotation()) {
      return new Verdict(
          CheckOutcome.INFO,
          "Total sum insured "
              + DisplayFormat.amount(tsi)
              + " is above "
              + DisplayFormat.amount(threshold)
              + ": For Quotation (TSU)",
          QUOTATION_DETAIL);
    }
    return tsi.compareTo(threshold) > 0
        ? Verdict.fail(
            "Total sum insured "
                + DisplayFormat.amount(tsi)
                + " is above "
                + DisplayFormat.amount(threshold)
                + ": TSU or proposal handling",
            "tsi " + tsi.toPlainString() + ", threshold " + threshold.toPlainString())
        : Verdict.pass("Total sum insured within " + DisplayFormat.amount(threshold));
  }

  /**
   * Whether a TSI above the threshold is Clean with For Quotation (BDOI's rule: the parameter
   * RNW_TSI_ABOVE_ROUTE is QUOTATION) rather than Review with a proposal (REVIEW).
   */
  private boolean quotation() {
    return "QUOTATION".equals(parameters.text("RNW_TSI_ABOVE_ROUTE", "QUOTATION").strip());
  }

  private BigDecimal threshold() {
    return settings
        .findByCheckCode(CODE)
        .map(CheckSetting::getParameters)
        .map(TsiThresholdCheck::parse)
        .orElse(DEFAULT_THRESHOLD);
  }

  /**
   * The threshold of the setting parameters ("250000000" or "amount=250000000").
   *
   * @param parameters parameters of the setting
   * @return threshold, the default when blank or unreadable
   */
  static BigDecimal parse(String parameters) {
    if (parameters == null || parameters.isBlank()) {
      return DEFAULT_THRESHOLD;
    }
    String value = parameters.contains("=") ? parameters.split("=", 2)[1] : parameters;
    try {
      return new BigDecimal(value.replace(",", "").strip());
    } catch (NumberFormatException e) {
      return DEFAULT_THRESHOLD;
    }
  }
}
