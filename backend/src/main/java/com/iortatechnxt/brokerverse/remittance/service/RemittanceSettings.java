package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Remittance parameters ({@code sys_parameter}, V760): the check holding period in banking days
 * {@code REMIT_CHECK_HOLD_DAYS} (RMTID.017), the paid AR above DTIP behaviour {@code
 * REMIT_PAIDAR_OVER_DTIP_MODE} = EXCLUDE or CAP (RMTID.014, OQ19). The naming of the extract files
 * moved to the Remittance Settings ({@link RemittanceSettingsService}); {@code REMIT_FILE_PATTERN}
 * is no longer read.
 */
@Component
public class RemittanceSettings {

  /** Module name on ledger movements, locks, events and hand-offs. */
  public static final String MODULE = "REMITTANCE";

  /** Check holding period parameter. */
  public static final String CHECK_HOLD_DAYS = "REMIT_CHECK_HOLD_DAYS";

  /** Paid AR above DTIP parameter. */
  public static final String OVER_DTIP_MODE = "REMIT_PAIDAR_OVER_DTIP_MODE";

  /** Withholding tax rate (percent) of the early-incentive service invoice (DIS 3.29.1). */
  public static final String EARLY_INCENTIVE_WTAX_RATE = "EARLY_INCENTIVE_WTAX_RATE";

  private static final BigDecimal DEFAULT_WTAX_RATE = BigDecimal.valueOf(2);

  private static final int DEFAULT_HOLD_DAYS = 3;
  private static final String EXCLUDE = "EXCLUDE";

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

  /** Treatment of paid AR above the DTIP balance (OQ19). */
  public enum OverDtipMode {
    /** Exclude the invoice from extraction (acceptance criteria of RMTID.014). */
    EXCLUDE,
    /** Remit only the DTIP balance (expected result of RMTID.014). */
    CAP
  }
}
