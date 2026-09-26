package com.iortatechnxt.brokerverse.brokerclaims.home.service;

import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Claims worklist (BRCLM.034/043, FR-CM-055): the claims of a company in the tabs My Claims,
 * Open, Temporarily Closed, Closed and Follow-ups Due, filtered by a home tile (overdue follow-up,
 * unpaid premium, awaiting premium remittance, status) and searched by claim number, insurer claim
 * number, ARN, policy number or assured. Ages are computed on read.
 */
@Service
@Transactional(readOnly = true)
public class WorklistQuery {

  private static final String OPEN = " c.phase <> 'CLOSED'";

  private static final String WHERE =
      " from bcl_claim c"
          + " left join lov_value s on s.type_code = 'BCL_CLAIM_STATUS' and s.code = c.status_code"
          + " where c.company_id = :companyId"
          + " and (:tab <> 'MINE' or (lower(c.handler) = lower(:me) and"
          + OPEN
          + "))"
          + " and (:tab <> 'OPEN' or c.phase in ('NEW', 'IN_PROGRESS'))"
          + " and (:tab <> 'TEMP_CLOSED' or c.phase = 'TEMP_CLOSED')"
          + " and (:tab <> 'CLOSED' or c.phase = 'CLOSED')"
          + " and (:tab <> 'FOLLOW_UPS_DUE' or ("
          + OPEN
          + " and (c.next_follow_up_date <= :today or exists (select 1 from bcl_diary_entry d"
          + " where d.claim_id = c.id and d.done_at is null and d.due_date <= :today))))"
          + " and (:flag <> 'OVERDUE' or ("
          + OPEN
          + " and c.next_follow_up_date < :today))"
          + " and (:flag <> 'UNPAID_PREMIUM' or ("
          + OPEN
          + " and c.premium_status in ('UNPAID', 'PARTIALLY_PAID')))"
          + " and (:flag <> 'AWAITING_REMITTANCE' or ("
          + OPEN
          + " and exists (select 1 from bcl_lov_attribute a where a.type_code = 'BCL_CLAIM_STATUS'"
          + " and a.code = c.status_code and a.attribute = 'awaiting_premium_remittance'"
          + " and lower(a.value) = 'true')))"
          + " and (cast(:status as varchar) is null or c.status_code = :status)"
          + " and (cast(:q as varchar) is null or c.claim_no ilike :like or c.arn ilike :like"
          + " or c.policy_no ilike :like or c.assured_name ilike :like"
          + " or exists (select 1 from bcl_insurer_claim x where x.claim_id = c.id"
          + " and x.insurer_claim_no ilike :like))";

  private static final String SELECT =
      "select c.id, c.claim_no, c.arn, c.policy_year, c.policy_no, c.assured_name,"
          + " c.claimant_name, c.handler, c.unit_code, c.status_code,"
          + " coalesce(s.label, c.status_code) as status_label, c.phase, c.closure_kind,"
          + " c.status_since, c.reported_date, c.loss_date, c.closed_on, c.next_follow_up_date,"
          + " c.next_action_plan, c.premium_status, c.currency, c.claim_amount,"
          + " (select string_agg(x.insurer_claim_no, ', ' order by x.id) from bcl_insurer_claim x"
          + " where x.claim_id = c.id and x.insurer_claim_no is not null) as insurer_claim_nos"
          + WHERE
          + " order by case when :tab = 'FOLLOW_UPS_DUE' then c.next_follow_up_date end nulls last,"
          + " c.reported_date desc, c.id desc limit :size offset :offset";

  private final NamedParameterJdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the query.
   *
   * @param jdbc named-parameter JDBC
   * @param currentUser current user
   * @param clock clock
   */
  public WorklistQuery(NamedParameterJdbcTemplate jdbc, CurrentUser currentUser, Clock clock) {
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * A page of the worklist.
   *
   * @param companyId company
   * @param criteria tab, tile filter, status and search text
   * @param page zero-based page
   * @param size page size
   * @return claims
   */
  public PageResponse<WorklistRow> search(
      Long companyId, WorklistCriteria criteria, int page, int size) {
    LocalDate today = ClaimAgeing.today(clock);
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", companyId);
    args.put("me", currentUser.username());
    args.put("today", today);
    args.put("tab", criteria.tab().name());
    args.put("flag", criteria.flag() == null ? "" : criteria.flag().name());
    args.put("status", blankToNull(criteria.statusCode()));
    String q = blankToNull(criteria.text());
    args.put("q", q);
    args.put("like", q == null ? "" : "%" + escape(q) + "%");
    args.put("size", size);
    args.put("offset", (long) page * size);
    List<WorklistRow> rows = jdbc.query(SELECT, args, (rs, n) -> row(rs, today));
    Long total = jdbc.queryForObject("select count(*)" + WHERE, args, Long.class);
    long count = total == null ? 0 : total;
    return new PageResponse<>(rows, page, size, count, (int) ((count + size - 1) / size));
  }

  private static WorklistRow row(ResultSet rs, LocalDate today) throws SQLException {
    LocalDate reported = date(rs.getDate("reported_date"));
    LocalDate closedOn = date(rs.getDate("closed_on"));
    Instant since = instant(rs.getTimestamp("status_since"));
    LocalDate followUp = date(rs.getDate("next_follow_up_date"));
    String phase = rs.getString("phase");
    return new WorklistRow(
        rs.getLong("id"),
        rs.getString("claim_no"),
        new WorklistRow.Cover(
            rs.getString("arn"),
            rs.getInt("policy_year"),
            rs.getString("policy_no"),
            rs.getString("assured_name"),
            rs.getString("claimant_name"),
            rs.getString("insurer_claim_nos")),
        new WorklistRow.Handling(
            rs.getString("handler"),
            rs.getString("unit_code"),
            rs.getString("status_code"),
            rs.getString("status_label"),
            phase,
            rs.getString("closure_kind")),
        new WorklistRow.Dates(
            reported,
            date(rs.getDate("loss_date")),
            followUp,
            ClaimAgeing.ageThisStage(since, today),
            reported == null ? 0 : ClaimAgeing.ageOverall(reported, closedOn, today),
            !"CLOSED".equals(phase) && followUp != null && followUp.isBefore(today)),
        rs.getString("next_action_plan"),
        rs.getString("premium_status"),
        rs.getString("currency"),
        rs.getBigDecimal("claim_amount"));
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  private static String escape(String text) {
    return text.replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
        .toLowerCase(Locale.ROOT);
  }

  private static LocalDate date(Date value) {
    return value == null ? null : value.toLocalDate();
  }

  private static Instant instant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  /** Tabs of the worklist. */
  public enum Tab {
    /** My open claims. */
    MINE,
    /** Claims in phase NEW or IN_PROGRESS. */
    OPEN,
    /** Temporarily closed claims. */
    TEMP_CLOSED,
    /** Permanently closed claims. */
    CLOSED,
    /** Open claims with the follow-up or a diary entry due today or overdue. */
    FOLLOW_UPS_DUE,
    /** Every claim. */
    ALL
  }

  /** Filters of the Claims home tiles. */
  public enum Flag {
    /** Follow-up date passed. */
    OVERDUE,
    /** Cover with unpaid or partly paid premium. */
    UNPAID_PREMIUM,
    /** Status awaiting premium remittance. */
    AWAITING_REMITTANCE
  }

  /**
   * Worklist criteria.
   *
   * @param tab tab
   * @param flag home tile filter, may be null
   * @param statusCode status, may be null
   * @param text search text, may be null
   */
  public record WorklistCriteria(Tab tab, Flag flag, String statusCode, String text) {}

  /**
   * A row of the worklist.
   *
   * @param id claim
   * @param claimNo claim number
   * @param cover cover, assured, claimant and insurer claim numbers
   * @param handling handler, unit, status and phase
   * @param dates reported and loss dates, follow-up and ages
   * @param nextActionPlan next action plan summary
   * @param premiumStatus premium check
   * @param currency currency
   * @param claimAmount claim amount
   */
  public record WorklistRow(
      Long id,
      String claimNo,
      Cover cover,
      Handling handling,
      Dates dates,
      String nextActionPlan,
      String premiumStatus,
      String currency,
      BigDecimal claimAmount) {

    /**
     * Cover of a row.
     *
     * @param arn account
     * @param policyYear policy year
     * @param policyNo policy number
     * @param assuredName assured
     * @param claimantName claimant
     * @param insurerClaimNos insurer claim numbers
     */
    public record Cover(
        String arn,
        int policyYear,
        String policyNo,
        String assuredName,
        String claimantName,
        String insurerClaimNos) {}

    /**
     * Handling of a row.
     *
     * @param handler handler
     * @param unitCode unit
     * @param statusCode status
     * @param statusLabel status label
     * @param phase phase
     * @param closureKind closure kind
     */
    public record Handling(
        String handler,
        String unitCode,
        String statusCode,
        String statusLabel,
        String phase,
        String closureKind) {}

    /**
     * Dates and ages of a row.
     *
     * @param reportedDate reported date
     * @param lossDate loss date
     * @param nextFollowUpDate next follow-up date
     * @param ageThisStage days in the current status
     * @param ageOverall days since reported
     * @param followUpOverdue open and past the follow-up date
     */
    public record Dates(
        LocalDate reportedDate,
        LocalDate lossDate,
        LocalDate nextFollowUpDate,
        int ageThisStage,
        int ageOverall,
        boolean followUpOverdue) {}
  }
}
