package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.system.service.SystemParameterService;
import com.iortatechnxt.finverse.tax.domain.IcLineItem;
import com.iortatechnxt.finverse.tax.domain.IcLineItemRepository;
import com.iortatechnxt.finverse.tax.domain.IcMeasure;
import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.domain.ReturnFigures;
import com.iortatechnxt.finverse.tax.service.IcScheduleResult.RbcSummary;
import com.iortatechnxt.finverse.tax.service.TaxSourceQueries.AccountAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the Insurance Commission schedules from the ledger through the authorized mapping
 * ({@link IcLineItem}); nothing is read from the reserves or investment modules, so the schedules
 * show exactly what is booked.
 *
 * <ul>
 *   <li>MOVEMENT lines: posted movement between the dates, excluding year-end closing journals.
 *       Per-LOB schedules split it by the line-of-business dimension of the ledger lines (lines
 *       without one appear under UNASSIGNED).
 *   <li>BALANCE lines: closing balance as of the end date.
 *   <li>RBC (simplified template, assumption): requirement = Σ line amount × RBC factor, without
 *       the covariance (square-root) aggregation of the full IC RBC2 framework; available capital =
 *       the net worth schedule total; ratio = available capital / requirement × 100, compared with
 *       the hurdle parameter {@code IC_RBC_HURDLE_PERCENT}.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class IcScheduleService {

  /** Parameter holding the minimum RBC ratio. */
  public static final String RBC_HURDLE = "IC_RBC_HURDLE_PERCENT";

  private static final String UNASSIGNED = "UNASSIGNED";
  private static final int DEFAULT_HURDLE = 100;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final IcLineItemRepository items;
  private final TaxSourceQueries queries;
  private final SystemParameterService parameters;

  /**
   * Creates the service.
   *
   * @param items mapping
   * @param queries ledger queries
   * @param parameters system parameters (RBC hurdle)
   */
  public IcScheduleService(
      IcLineItemRepository items, TaxSourceQueries queries, SystemParameterService parameters) {
    this.items = items;
    this.queries = queries;
    this.parameters = parameters;
  }

  /**
   * Computes a schedule.
   *
   * @param companyId company
   * @param schedule schedule
   * @param from period start (movement lines)
   * @param asOf period end and balance date
   * @return schedule result
   */
  public IcScheduleResult compute(
      Long companyId, IcSchedule schedule, LocalDate from, LocalDate asOf) {
    List<IcLineItem> lines =
        items.findByCompanyIdAndScheduleAndRecordStatusOrderByLineOrder(
            companyId, schedule, RecordStatus.ACTIVE);
    boolean needMovements = lines.stream().anyMatch(l -> l.getMeasure() == IcMeasure.MOVEMENT);
    boolean needBalances = lines.stream().anyMatch(l -> l.getMeasure() == IcMeasure.BALANCE);
    List<AccountAmount> movements =
        needMovements ? queries.movements(companyId, from, asOf) : List.of();
    List<AccountAmount> balances = needBalances ? queries.balances(companyId, asOf) : List.of();
    List<IcScheduleLine> out = new ArrayList<>();
    for (IcLineItem item : lines) {
      List<AccountAmount> source = item.getMeasure() == IcMeasure.MOVEMENT ? movements : balances;
      if (schedule.byLineOfBusiness()) {
        byBusinessLine(item, source).forEach((lob, net) -> out.add(line(item, lob, net)));
      } else {
        out.add(line(item, null, net(item, source)));
      }
    }
    BigDecimal total = WorksheetSupport.sum(out, IcScheduleLine::signedAmount);
    RbcSummary rbc = schedule == IcSchedule.RBC ? rbc(companyId, from, asOf, out) : null;
    return new IcScheduleResult(schedule, from, asOf, out, total, rbc);
  }

  private RbcSummary rbc(
      Long companyId, LocalDate from, LocalDate asOf, List<IcScheduleLine> lines) {
    BigDecimal requirement = WorksheetSupport.sum(lines, IcScheduleLine::requirement);
    BigDecimal netWorth = compute(companyId, IcSchedule.NET_WORTH, from, asOf).total();
    BigDecimal ratio =
        requirement.signum() == 0
            ? null
            : netWorth.multiply(HUNDRED).divide(requirement, 2, RoundingMode.HALF_EVEN);
    BigDecimal hurdle = BigDecimal.valueOf(parameters.intValue(RBC_HURDLE, DEFAULT_HURDLE));
    return new RbcSummary(netWorth, requirement, ratio, hurdle);
  }

  private static Map<String, BigDecimal> byBusinessLine(
      IcLineItem item, List<AccountAmount> source) {
    Map<String, BigDecimal> out = new TreeMap<>();
    for (AccountAmount a : source) {
      if (item.matches(a.accountCode(), a.reportGroup())) {
        String lob = a.businessLine() == null ? UNASSIGNED : a.businessLine();
        out.merge(lob, a.net(), BigDecimal::add);
      }
    }
    return out;
  }

  private static BigDecimal net(IcLineItem item, List<AccountAmount> source) {
    return source.stream()
        .filter(a -> item.matches(a.accountCode(), a.reportGroup()))
        .map(AccountAmount::net)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static IcScheduleLine line(IcLineItem item, String businessLine, BigDecimal net) {
    BigDecimal amount = Money.round(item.getNormalBalance().present(net));
    BigDecimal factor = item.getRbcFactor();
    return new IcScheduleLine(
        item.getLineCode(),
        item.getDescription(),
        item.mappingText(),
        businessLine,
        amount,
        amount.multiply(BigDecimal.valueOf(item.getSignFactor())),
        factor,
        factor == null ? null : ReturnFigures.percentOf(amount, factor));
  }
}
