package com.iortatechnxt.brokerverse.nbreport.service;

import com.iortatechnxt.brokerverse.nbreport.domain.SalesTarget;
import com.iortatechnxt.brokerverse.nbreport.domain.SalesTargetRepository;
import com.iortatechnxt.brokerverse.nbreport.domain.UnitLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production statistics per sales unit against targets (BRNB.075): bookings, premium and commission
 * of the booked invoices dated in the period, attributed to the region, department, team or account
 * officer stamped on the account, next to the unit's targets pro rata to the days of the period
 * they cover. Units with a target but no production are listed too.
 */
@Service
@Transactional(readOnly = true)
public class ProductionService {

  /** Shown for bookings of accounts without a sales stamp. */
  public static final String UNASSIGNED = "(unassigned)";

  private static final String PRODUCTION =
      "select coalesce(case cast(:level as varchar) when 'REGION' then a.sales_region"
          + " when 'DEPARTMENT' then a.sales_department when 'TEAM' then a.sales_team"
          + " else a.account_officer end, '"
          + UNASSIGNED
          + "') as unit,"
          + " count(*) filter (where i.kind = 'BOOKING') as bookings,"
          + " coalesce(sum(i.basic_premium), 0) as premium,"
          + " coalesce(sum(i.commission), 0) as commission"
          + " from bkg_invoice i join acc_account a on a.id = i.account_id"
          + " where i.company_id = :company and i.status = 'BOOKED'"
          + " and i.booking_date between :from and :to"
          + " group by 1";

  private static final String UNIT_NAMES =
      "select code, name from cat_sales_unit where company_id = :company"
          + " union all select username, full_name from sec_user";

  private final NbReportJdbc jdbc;
  private final SalesTargetRepository targets;

  /**
   * Creates the service.
   *
   * @param jdbc report SQL
   * @param targets production targets
   */
  public ProductionService(NbReportJdbc jdbc, SalesTargetRepository targets) {
    this.jdbc = jdbc;
    this.targets = targets;
  }

  /**
   * Production and targets of every unit of a level in a period.
   *
   * @param companyId company
   * @param level unit level
   * @param from period start
   * @param to period end
   * @return units by code
   */
  public List<UnitProduction> production(
      long companyId, UnitLevel level, LocalDate from, LocalDate to) {
    Map<String, Object> args =
        SqlArgs.company(companyId)
            .with("level", level.name())
            .with("from", from)
            .with("to", to)
            .map();
    Map<String, Totals> units = new TreeMap<>();
    for (Map<String, Object> row : jdbc.rows(PRODUCTION, args)) {
      Totals t = units.computeIfAbsent((String) row.get("unit"), k -> new Totals());
      t.bookings += ((Number) row.get("bookings")).longValue();
      t.premium = t.premium.add((BigDecimal) row.get("premium"));
      t.commission = t.commission.add((BigDecimal) row.get("commission"));
    }
    for (SalesTarget target : targets.overlapping(companyId, from, to)) {
      if (target.getUnitLevel() == level) {
        units.computeIfAbsent(target.getUnitCode(), k -> new Totals()).add(target, from, to);
      }
    }
    Map<String, String> names = names(companyId);
    List<UnitProduction> out = new ArrayList<>();
    units.forEach(
        (code, t) ->
            out.add(
                new UnitProduction(
                    level,
                    code,
                    names.getOrDefault(code, code),
                    t.bookings,
                    t.premium,
                    t.commission,
                    t.targetCount.setScale(0, RoundingMode.HALF_UP).longValue(),
                    t.targetPremium.setScale(2, RoundingMode.HALF_UP),
                    t.targetCommission.setScale(2, RoundingMode.HALF_UP))));
    return out;
  }

  private Map<String, String> names(long companyId) {
    Map<String, String> names = new TreeMap<>();
    jdbc.rows(UNIT_NAMES, SqlArgs.company(companyId).map())
        .forEach(r -> names.putIfAbsent((String) r.get("code"), (String) r.get("name")));
    return names;
  }

  /** Accumulated production and pro-rata targets of a unit. */
  private static final class Totals {
    private long bookings;
    private BigDecimal premium = BigDecimal.ZERO;
    private BigDecimal commission = BigDecimal.ZERO;
    private BigDecimal targetCount = BigDecimal.ZERO;
    private BigDecimal targetPremium = BigDecimal.ZERO;
    private BigDecimal targetCommission = BigDecimal.ZERO;

    /** Adds the share of a target that falls in the period (by days). */
    void add(SalesTarget target, LocalDate from, LocalDate to) {
      LocalDate start = target.getPeriodFrom().isAfter(from) ? target.getPeriodFrom() : from;
      LocalDate end = target.getPeriodTo().isBefore(to) ? target.getPeriodTo() : to;
      BigDecimal share =
          BigDecimal.valueOf(ChronoUnit.DAYS.between(start, end) + 1)
              .divide(
                  BigDecimal.valueOf(
                      ChronoUnit.DAYS.between(target.getPeriodFrom(), target.getPeriodTo()) + 1),
                  10,
                  RoundingMode.HALF_UP);
      targetCount = targetCount.add(BigDecimal.valueOf(target.getTargetCount()).multiply(share));
      targetPremium = targetPremium.add(target.getTargetPremium().multiply(share));
      targetCommission = targetCommission.add(target.getTargetCommission().multiply(share));
    }
  }
}
