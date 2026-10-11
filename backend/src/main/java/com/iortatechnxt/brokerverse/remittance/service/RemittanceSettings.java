package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Remittance parameters ({@code sys_parameter}, V760): the check holding period in banking days
 * {@code REMIT_CHECK_HOLD_DAYS} (RMTID.017), the paid AR above DTIP behaviour {@code
 * REMIT_PAIDAR_OVER_DTIP_MODE} = EXCLUDE or CAP (RMTID.014, OQ19) and the extract file naming
 * {@code REMIT_FILE_PATTERN} (RMTID.001, OQ17/OQ18).
 */
@Component
public class RemittanceSettings {

  /** Module name on ledger movements, locks, events and hand-offs. */
  public static final String MODULE = "REMITTANCE";

  /** Check holding period parameter. */
  public static final String CHECK_HOLD_DAYS = "REMIT_CHECK_HOLD_DAYS";

  /** Paid AR above DTIP parameter. */
  public static final String OVER_DTIP_MODE = "REMIT_PAIDAR_OVER_DTIP_MODE";

  /** Extract file naming parameter. */
  public static final String FILE_PATTERN = "REMIT_FILE_PATTERN";

  /** Withholding tax rate (percent) of the early-incentive service invoice (DIS 3.29.1). */
  public static final String EARLY_INCENTIVE_WTAX_RATE = "EARLY_INCENTIVE_WTAX_RATE";

  private static final BigDecimal DEFAULT_WTAX_RATE = BigDecimal.valueOf(2);

  private static final int DEFAULT_HOLD_DAYS = 3;
  private static final String EXCLUDE = "EXCLUDE";
  private static final String DEFAULT_PATTERN = "{batchNo}_{date}";

  private final SystemParameterService parameters;

  /**
   * Creates the settings.
   *
   * @param parameters business parameters
   */
  public RemittanceSettings(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Banking days a payment is held before it may be remitted.
   *
   * @return days
   */
  public int checkHoldDays() {
    return parameters.intValue(CHECK_HOLD_DAYS, DEFAULT_HOLD_DAYS);
  }

  /**
   * Whether paid AR above the DTIP balance is capped at the DTIP instead of excluded (OQ19).
   *
   * @return true for CAP
   */
  public boolean capOverDtip() {
    String value = parameters.text(OVER_DTIP_MODE, EXCLUDE).strip().toUpperCase(Locale.ROOT);
    try {
      return OverDtipMode.valueOf(value) == OverDtipMode.CAP;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  /**
   * Withholding tax rate the insurer applies on the early incentive (DIS 3.29.1, AQ25).
   *
   * @return rate in percent, 2 when the parameter is missing or not a number
   */
  public BigDecimal earlyIncentiveWtaxRate() {
    String value = parameters.text(EARLY_INCENTIVE_WTAX_RATE, DEFAULT_WTAX_RATE.toPlainString());
    try {
      return new BigDecimal(value.strip());
    } catch (NumberFormatException ex) {
      return DEFAULT_WTAX_RATE;
    }
  }

  /**
   * File name of a batch extract: the pattern with {@code {batchNo}}, {@code {insurer}}, {@code
   * {type}} and {@code {date}} replaced, plus the extension.
   *
   * @param batchNo batch
   * @param insurer insurer
   * @param type remittance type
   * @param date business date (ISO)
   * @return file name
   */
  public String extractFileName(String batchNo, String insurer, String type, String date) {
    String pattern = parameters.text(FILE_PATTERN, DEFAULT_PATTERN).strip();
    String base = pattern.isEmpty() ? DEFAULT_PATTERN : pattern;
    return base.replace("{batchNo}", batchNo)
            .replace("{insurer}", insurer)
            .replace("{type}", type)
            .replace("{date}", date)
            .replaceAll("[^A-Za-z0-9._-]", "_")
        + ".xlsx";
  }

  /** Treatment of paid AR above the DTIP balance (OQ19). */
  public enum OverDtipMode {
    /** Exclude the invoice from extraction (acceptance criteria of RMTID.014). */
    EXCLUDE,
    /** Remit only the DTIP balance (expected result of RMTID.014). */
    CAP
  }
}
