package com.iortatechnxt.finverse.budget.api.dto;

import com.iortatechnxt.finverse.budget.service.VarianceLine;
import com.iortatechnxt.finverse.coa.domain.AccountClass;
import java.math.BigDecimal;

/**
 * Budget vs actual line (natural sign).
 *
 * @param accountCode account
 * @param accountName name
 * @param accountClass INCOME or EXPENSE
 * @param costCenter cost centre, if split
 * @param budgetMonth month budget
 * @param actualMonth month actual
 * @param monthVariance month variance (actual − budget)
 * @param monthVariancePct month variance percent
 * @param budgetYtd YTD budget
 * @param actualYtd YTD actual
 * @param ytdVariance YTD variance
 * @param ytdVariancePct YTD variance percent
 * @param annualBudget annual budget
 * @param utilizationPct YTD actual in percent of annual budget
 * @param available remaining annual budget
 * @param favourable whether the YTD variance is favourable
 */
public record VarianceLineResponse(
    String accountCode,
    String accountName,
    AccountClass accountClass,
    String costCenter,
    BigDecimal budgetMonth,
    BigDecimal actualMonth,
    BigDecimal monthVariance,
    BigDecimal monthVariancePct,
    BigDecimal budgetYtd,
    BigDecimal actualYtd,
    BigDecimal ytdVariance,
    BigDecimal ytdVariancePct,
    BigDecimal annualBudget,
    BigDecimal utilizationPct,
    BigDecimal available,
    boolean favourable) {

  /**
   * Maps a line.
   *
   * @param l line
   * @return response
   */
  public static VarianceLineResponse from(VarianceLine l) {
    return new VarianceLineResponse(
        l.accountCode(),
        l.accountName(),
        l.accountClass(),
        l.costCenter(),
        l.budgetMonth(),
        l.actualMonth(),
        l.monthVariance(),
        l.monthVariancePct(),
        l.budgetYtd(),
        l.actualYtd(),
        l.ytdVariance(),
        l.ytdVariancePct(),
        l.annualBudget(),
        l.utilizationPct(),
        l.available(),
        l.favourable());
  }
}
