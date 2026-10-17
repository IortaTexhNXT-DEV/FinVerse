package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps an account to its CSF status (BRCSF-005 / 5.001, CSF-EM07): the account stage, whether its
 * premium receivable is outstanding and whether its policy period has ended, through the
 * maintainable list CSF_STATUS_MAP (read on every call, so a changed mapping applies at once).
 */
@Component
public class CsfStatusMapper {

  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the mapper.
   *
   * @param lovs lists of values
   * @param clock clock
   */
  public CsfStatusMapper(LovService lovs, Clock clock) {
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * The current mapping rules.
   *
   * @return rules in order
   */
  public CsfStatusRules rules() {
    LocalDate today = BusinessClock.today(clock);
    return CsfStatusRules.of(
        lovs.activeValues(CsfCodes.LOV_STATUS_MAP, today).stream()
            .sorted(Comparator.comparingInt(LovValue::getSortOrder))
            .map(v -> new CsfStatusRules.Row(v.getCode(), v.getParentCode()))
            .toList());
  }

  /**
   * The CSF status of an account.
   *
   * @param rules current rules
   * @param account account
   * @param premiumBalance outstanding premium receivable of its invoices
   * @return CSF status code, empty when no row applies
   */
  public Optional<String> statusOf(
      CsfStatusRules rules, Account account, BigDecimal premiumBalance) {
    LocalDate today = BusinessClock.today(clock);
    boolean expired = account.getPeriodTo() != null && account.getPeriodTo().isBefore(today);
    boolean outstanding = premiumBalance != null && premiumBalance.signum() > 0;
    return rules.statusOf(account.getStatus(), new CsfStatusRules.Facts(outstanding, expired));
  }
}
