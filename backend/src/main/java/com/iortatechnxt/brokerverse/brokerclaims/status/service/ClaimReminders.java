package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The daily reminders of the Claims jobs (BRCLM.019/022/031/034; CLAIMS_BROKING_DESIGN 9.1-9.2):
 * finds the open claims whose next follow-up date or diary due date is today (notice {@code
 * BCL_FOLLOW_UP_DUE} to the handler or assignee) or has passed (alert {@code
 * BCL_FOLLOW_UP_OVERDUE}, one per claim until resolved), and the outstanding claims older than
 * {@code BCL_PAST_DUE_DAYS} (alert {@code BCL_CLAIM_PAST_DUE}, one per claim, with a notice to the
 * handler).
 */
@Service
@Transactional
public class ClaimReminders {

  /** Notification event of the follow-up reminder. */
  static final String FOLLOW_UP_DUE_EVENT = "BCL_FOLLOW_UP_DUE";

  /** Alert of an overdue follow-up or diary entry. */
  static final String FOLLOW_UP_OVERDUE = "BCL_FOLLOW_UP_OVERDUE";

  /** Alert of an outstanding claim past the threshold. */
  static final String CLAIM_PAST_DUE = "BCL_CLAIM_PAST_DUE";

  private static final String CLAIM_LINK = "/claims-handling/";

  private static final String FOLLOW_UPS =
      "select c.id, c.company_id, c.branch_id, c.claim_no, c.handler as recipient,"
          + " c.next_follow_up_date as due, 'Follow-up' as kind"
          + " from bcl_claim c where c.phase <> 'CLOSED' and c.next_follow_up_date <= :today"
          + " union all"
          + " select c.id, c.company_id, c.branch_id, c.claim_no,"
          + " coalesce(d.assignee, d.created_by) as recipient, d.due_date as due, 'Diary' as kind"
          + " from bcl_diary_entry d join bcl_claim c on c.id = d.claim_id"
          + " where d.done_at is null and d.due_date <= :today"
          + " order by 1";

  private static final String PAST_DUE =
      "select c.id, c.company_id, c.branch_id, c.claim_no, c.handler as recipient,"
          + " (cast(:today as date) - c.reported_date) as age"
          + " from bcl_claim c where c.phase <> 'CLOSED'"
          + " and (cast(:today as date) - c.reported_date) > :days order by c.id";

  private final NamedParameterJdbcTemplate jdbc;
  private final NotificationService notifications;
  private final AlertService alerts;

  /**
   * Creates the reminders.
   *
   * @param jdbc named-parameter JDBC
   * @param notifications in-app notifications
   * @param alerts alert engine
   */
  public ClaimReminders(
      NamedParameterJdbcTemplate jdbc, NotificationService notifications, AlertService alerts) {
    this.jdbc = jdbc;
    this.notifications = notifications;
    this.alerts = alerts;
  }

  /**
   * Follow-ups and diary entries due today or overdue.
   *
   * @param today business date
   * @return rows: claim, company, branch, number, recipient, due date and kind
   */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> followUps(LocalDate today) {
    return jdbc.queryForList(FOLLOW_UPS, Map.of("today", today));
  }

  /**
   * Outstanding claims older than the threshold.
   *
   * @param today business date
   * @param days past due threshold in days
   * @return rows: claim, company, branch, number, handler and age
   */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> pastDue(LocalDate today, int days) {
    return jdbc.queryForList(PAST_DUE, Map.of("today", today, "days", days));
  }

  /**
   * Reminds of one follow-up or diary entry: a notice when due today, an alert when overdue.
   *
   * @param row a row of {@link #followUps}
   * @param today business date
   * @return true when a notice was sent or an alert raised
   */
  public boolean remind(Map<String, Object> row, LocalDate today) {
    LocalDate due = ((Date) row.get("due")).toLocalDate();
    String claimNo = (String) row.get("claim_no");
    String kind = (String) row.get("kind");
    if (due.isEqual(today)) {
      return notifications.notifyUser(
          (String) row.get("recipient"),
          notice(row, claimNo + ": " + kind.toLowerCase(Locale.ROOT) + " due today"),
          FOLLOW_UP_DUE_EVENT);
    }
    return alerts
        .raise(
            FOLLOW_UP_OVERDUE,
            facts(row, claimNo + ": " + kind + " overdue since " + due, FOLLOW_UP_OVERDUE))
        .isPresent();
  }

  /**
   * Raises the past due alert of one claim, and tells its handler when it is new.
   *
   * @param row a row of {@link #pastDue}
   * @param days threshold
   * @return true when a new alert was raised
   */
  public boolean flagPastDue(Map<String, Object> row, int days) {
    String claimNo = (String) row.get("claim_no");
    String text = claimNo + " is outstanding for " + row.get("age") + " days (over " + days + ")";
    boolean raised = alerts.raise(CLAIM_PAST_DUE, facts(row, text, CLAIM_PAST_DUE)).isPresent();
    if (raised) {
      notifications.notifyUser((String) row.get("recipient"), notice(row, text));
    }
    return raised;
  }

  private static AlertFacts facts(Map<String, Object> row, String message, String code) {
    Object claimId = row.get("id");
    return new AlertFacts(
        ((Number) row.get("company_id")).longValue(),
        row.get("branch_id") == null ? null : ((Number) row.get("branch_id")).longValue(),
        ClaimCodes.ENTITY_TYPE,
        String.valueOf(claimId),
        message,
        null,
        code + ":" + claimId);
  }

  private static Notice notice(Map<String, Object> row, String title) {
    Object claimId = row.get("id");
    return new Notice(
        title,
        "Open the claim to follow it up.",
        CLAIM_LINK + claimId,
        ClaimCodes.ENTITY_TYPE,
        String.valueOf(claimId));
  }
}
