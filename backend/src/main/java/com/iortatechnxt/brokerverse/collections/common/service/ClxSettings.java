package com.iortatechnxt.brokerverse.collections.common.service;

import com.iortatechnxt.brokerverse.collections.common.domain.AgingBrackets;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * The Collections business parameters (V1000, COLLECTIONS_DESIGN section 8), read at each use so a
 * change on Collections Setup applies at once.
 */
@Component
public class ClxSettings {

  /** Net outstanding PR above which an invoice is listed (BRCLXN.005-007, CQ03). */
  public static final String MIN_BALANCE_THRESHOLD = "CLX_MIN_BALANCE_THRESHOLD";

  /** Aging basis: BOOKING or INCEPTION (BRCLXN.049, 060). */
  public static final String AGING_BASIS = "CLX_AGING_BASIS";

  /** Aging brackets (OQ43). */
  public static final String AGING_BRACKETS = "CLX_AGING_BRACKETS";

  /** Row cap of an export (caveat p.93). */
  public static final String EXPORT_MAX_ROWS = "CLX_EXPORT_MAX_ROWS";

  /** Lifetime of the "is editing" lock in minutes (NFR record lock). */
  public static final String EDIT_LOCK_MINUTES = "CLX_EDIT_LOCK_MINUTES";

  private static final String DEFAULT_THRESHOLD = "10.00";
  private static final int DEFAULT_EXPORT_ROWS = 50_000;
  private static final int DEFAULT_LOCK_MINUTES = 15;
  private static final String INCEPTION = "INCEPTION";

  private final SystemParameterService parameters;

  /**
   * Creates the settings.
   *
   * @param parameters business parameters
   */
  public ClxSettings(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * The listing threshold.
   *
   * @return amount, scale 2
   */
  public BigDecimal threshold() {
    return new BigDecimal(parameters.text(MIN_BALANCE_THRESHOLD, DEFAULT_THRESHOLD).strip())
        .setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * The aging brackets.
   *
   * @return brackets
   */
  public AgingBrackets brackets() {
    return AgingBrackets.parse(parameters.items(AGING_BRACKETS));
  }

  /**
   * Age of an invoice on a date, by the aging basis.
   *
   * @param bookingDate booking date
   * @param inceptionDate inception date
   * @param today business date
   * @return days
   */
  public int ageInDays(LocalDate bookingDate, LocalDate inceptionDate, LocalDate today) {
    boolean fromInception =
        String.CASE_INSENSITIVE_ORDER.compare(
                INCEPTION, parameters.text(AGING_BASIS, "BOOKING").strip())
            == 0;
    LocalDate basis = fromInception ? inceptionDate : bookingDate;
    return (int) ChronoUnit.DAYS.between(basis, today);
  }

  /**
   * The row cap of an export.
   *
   * @return rows
   */
  public int exportMaxRows() {
    return parameters.intValue(EXPORT_MAX_ROWS, DEFAULT_EXPORT_ROWS);
  }

  /**
   * The lifetime of the edit lock.
   *
   * @return duration
   */
  public Duration editLock() {
    return Duration.ofMinutes(parameters.intValue(EDIT_LOCK_MINUTES, DEFAULT_LOCK_MINUTES));
  }
}
