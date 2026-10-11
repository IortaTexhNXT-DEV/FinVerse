package com.iortatechnxt.brokerverse.renewal.budget.service;

import com.iortatechnxt.brokerverse.renewal.budget.service.RenewalBudgetService.BudgetInput;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudget;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Validations of an Annual Renewal Budget record (BDOI Renewal FRS FRRN.042.04): the fiscal year
 * and the market segment are required, the region for the segments other than Corporate, the team
 * and sub-team for Corporate ({@value #CORPORATE_SEGMENTS}); the amounts cannot be negative; the
 * Account Officer is not required for CBG.
 */
@Component
public class RenewalBudgetValidator {

  /** Parameter: segments budgeted by team and sub-team. */
  public static final String CORPORATE_SEGMENTS = "RNW_CORPORATE_SEGMENTS";

  private static final int FIRST_YEAR = 2000;
  private static final int LAST_YEAR = 2100;

  private final SystemParameterService parameters;

  /**
   * Creates the validator.
   *
   * @param parameters system parameters
   */
  public RenewalBudgetValidator(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * The validation messages of a budget record; empty when it can be saved.
   *
   * @param input record
   * @return messages
   */
  public List<String> validate(BudgetInput input) {
    List<String> errors = new ArrayList<>();
    RenewalBudget.Key key = input.key();
    if (key.fiscalYear() < FIRST_YEAR || key.fiscalYear() > LAST_YEAR) {
      errors.add("Fiscal Year is required");
    }
    errors.addAll(segmentErrors(key));
    if (!RenewalBudgetService.PREMIUM.equals(key.measure())
        && !RenewalBudgetService.COMMISSION.equals(key.measure())) {
      errors.add("Measure must be Basic Premium or Gross Commission");
    }
    input.months().forEach(m -> errors.addAll(monthErrors(m)));
    return errors;
  }

  private static List<String> monthErrors(RenewalBudget.Month m) {
    List<String> errors = new ArrayList<>();
    if (m.monthNo() < 1 || m.monthNo() > RenewalBudget.MONTHS) {
      errors.add("Month must be 1 to 12");
    } else if (negative(m.newAmount())
        || negative(m.renewalAmount())
        || negative(m.organicAmount())) {
      errors.add(
          "Budget values cannot be negative (" + RenewalBudgetService.monthName(m.monthNo()) + ")");
    }
    return errors;
  }

  private List<String> segmentErrors(RenewalBudget.Key key) {
    if (blank(key.segment())) {
      return List.of("Market Segment is required");
    }
    if (corporate(key.segment())) {
      return blank(key.team()) || blank(key.subTeam())
          ? List.of("Team and Sub-Team are required for the Corporate segments")
          : List.of();
    }
    return blank(key.region())
        ? List.of("Region is required for the segments other than Corporate")
        : List.of();
  }

  /**
   * Whether a segment is budgeted by team and sub-team.
   *
   * @param segment segment
   * @return true for the Corporate segments
   */
  public boolean corporate(String segment) {
    return segment != null
        && parameters.items(CORPORATE_SEGMENTS).stream()
            .anyMatch(s -> s.strip().equals(segment.strip()));
  }

  private static boolean negative(BigDecimal v) {
    return v != null && v.signum() < 0;
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }
}
