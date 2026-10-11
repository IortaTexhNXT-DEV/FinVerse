package com.iortatechnxt.brokerverse.renewal.home.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.report.RenewalReportSupport;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renewal Home (FR-RN-100): the renewals of the user's scope by stage and bucket, the expiries of
 * the next 30, 60, 90 and 140 days, the renewals at risk, the exceptions, the insurer batches past
 * their reply date, the letters not delivered, and the workload per officer.
 */
@Service
@Transactional(readOnly = true)
public class RenewalHomeService {

  private static final String STAGES =
      "select c.stage as code, count(*) as n from rnw_candidate c where 1 = 1"
          + RenewalReportSupport.FILTERS
          + " group by c.stage";

  private static final String BUCKETS =
      "select c.bucket as code, count(*) as n from rnw_candidate c"
          + " where c.stage not in ('RENEWED', 'CLOSED') and c.bucket is not null"
          + RenewalReportSupport.FILTERS
          + " group by c.bucket";

  private static final String TILES =
      "select count(*) filter (where c.expiry_date between :today and :d30) as due30,"
          + " count(*) filter (where c.expiry_date between :today and :d60) as due60,"
          + " count(*) filter (where c.expiry_date between :today and :d90) as due90,"
          + " count(*) filter (where c.expiry_date between :today and :d140) as due140,"
          + " count(*) filter (where c.urgent) as urgent,"
          + " count(*) filter (where c.returned) as returned,"
          + " count(*) filter (where c.nrns) as nrns,"
          + " count(*) filter (where c.attention_flag is not null) as at_risk"
          + " from rnw_candidate c where c.stage not in ('RENEWED', 'CLOSED')"
          + RenewalReportSupport.FILTERS;

  private static final String OVERDUE =
      "select count(*) from rnw_insurer_batch b where b.company_id = :company"
          + " and b.status in ('SENT', 'PARTIALLY_RESPONDED') and b.reply_due < :today";

  private static final String FAILED =
      "select count(*) from rnw_letter l where l.company_id = :company and l.status = 'FAILED'";

  private static final String WORKLOAD =
      "select coalesce(c.assigned_ao, c.assigned_po) as officer,"
          + " case when c.assigned_po is not null then 'PO' else 'AO' end as role, count(*) as n"
          + " from rnw_candidate c where c.stage not in ('RENEWED', 'CLOSED', 'EXTRACTED')"
          + " and coalesce(c.assigned_ao, c.assigned_po) is not null"
          + RenewalReportSupport.FILTERS
          + " group by 1, 2 order by 3 desc limit 20";

  private static final int D30 = 30;
  private static final int D60 = 60;
  private static final int D90 = 90;
  private static final int D140 = 140;

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param jdbc SQL
   * @param support filters and scope
   * @param clock clock
   */
  public RenewalHomeService(NbReportJdbc jdbc, RenewalReportSupport support, Clock clock) {
    this.jdbc = jdbc;
    this.support = support;
    this.clock = clock;
  }

  /**
   * The home figures of the user's scope.
   *
   * @param companyId company
   * @return figures
   */
  public Home home(long companyId) {
    LocalDate today = BusinessClock.today(clock);
    Map<String, Object> args =
        support
            .allOf(companyId)
            .with("today", today)
            .with("d30", today.plusDays(D30))
            .with("d60", today.plusDays(D60))
            .with("d90", today.plusDays(D90))
            .with("d140", today.plusDays(D140))
            .map();
    List<Count> stages =
        jdbc.rows(STAGES, args).stream()
            .map(r -> count(r, RenewalStage.valueOf((String) r.get("code")).label()))
            .toList();
    List<Count> buckets =
        jdbc.rows(BUCKETS, args).stream()
            .map(r -> count(r, Bucket.valueOf((String) r.get("code")).label()))
            .toList();
    Map<String, Object> t = jdbc.rows(TILES, args).get(0);
    List<Officer> workload =
        jdbc.rows(WORKLOAD, args).stream()
            .map(r -> new Officer((String) r.get("officer"), (String) r.get("role"), n(r.get("n"))))
            .toList();
    return new Home(
        stages,
        buckets,
        new Tiles(
            n(t.get("due30")),
            n(t.get("due60")),
            n(t.get("due90")),
            n(t.get("due140")),
            n(t.get("at_risk")),
            n(t.get("urgent")),
            n(t.get("returned")),
            n(t.get("nrns")),
            jdbc.count(OVERDUE, args),
            jdbc.count(FAILED, args)),
        workload);
  }

  private static Count count(Map<String, Object> r, String label) {
    return new Count((String) r.get("code"), label, n(r.get("n")));
  }

  private static long n(Object value) {
    return value == null ? 0 : ((Number) value).longValue();
  }

  /**
   * Home figures.
   *
   * @param stages renewals by stage
   * @param buckets open renewals by bucket
   * @param tiles counters
   * @param workload open renewals per officer
   */
  public record Home(
      List<Count> stages, List<Count> buckets, Tiles tiles, List<Officer> workload) {}

  /**
   * A count.
   *
   * @param code code
   * @param label name
   * @param count count
   */
  public record Count(String code, String label, long count) {}

  /**
   * Counters of the home tiles.
   *
   * @param due30 expiring within 30 days
   * @param due60 within 60 days
   * @param due90 within 90 days
   * @param due140 within 140 days
   * @param atRisk renewals with an attention flag (ageing, overdue, high risk; FR-RN-102)
   * @param urgent urgent (go-live window)
   * @param returned returned to Marketing
   * @param nrns NRNS
   * @param insurerOverdue insurer batches past their reply date
   * @param lettersFailed letters not delivered
   */
  public record Tiles(
      long due30,
      long due60,
      long due90,
      long due140,
      long atRisk,
      long urgent,
      long returned,
      long nrns,
      long insurerOverdue,
      long lettersFailed) {}

  /**
   * Open renewals of an officer.
   *
   * @param username officer
   * @param role AO or PO
   * @param count renewals
   */
  public record Officer(String username, String role, long count) {}
}
