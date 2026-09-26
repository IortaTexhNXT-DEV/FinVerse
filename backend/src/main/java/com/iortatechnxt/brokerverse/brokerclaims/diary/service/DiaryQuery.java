package com.iortatechnxt.brokerverse.brokerclaims.diary.service;

import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Diary (BRCLM.022/034, FR-CM-052): the diary entries assigned to the signed-in user across the
 * claims of the company, open entries first by due date (overdue and due today on top), with the
 * claim number and assured.
 */
@Service
@Transactional(readOnly = true)
public class DiaryQuery {

  private static final String FROM =
      " from bcl_diary_entry d join bcl_claim c on c.id = d.claim_id"
          + " left join lov_value t on t.type_code = 'BCL_DIARY_TYPE' and t.code = d.entry_type"
          + " where c.company_id = :companyId and lower(d.assignee) = lower(:me)"
          + " and (cast(:all as boolean) or d.done_at is null)";

  private static final String SELECT =
      "select d.id, d.claim_id, c.claim_no, c.assured_name, d.entry_type,"
          + " coalesce(t.label, d.entry_type) as type_label, d.entry_date, d.due_date, d.assignee,"
          + " d.text, d.done_at, d.done_by, d.created_by"
          + FROM
          + " order by (d.done_at is not null), d.due_date nulls last, d.entry_date, d.id"
          + " limit :size offset :offset";

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
  public DiaryQuery(NamedParameterJdbcTemplate jdbc, CurrentUser currentUser, Clock clock) {
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The entries assigned to the signed-in user.
   *
   * @param companyId company
   * @param includeDone whether completed entries are listed too
   * @param page zero-based page
   * @param size page size
   * @return entries
   */
  public PageResponse<DiaryItem> mine(Long companyId, boolean includeDone, int page, int size) {
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", companyId);
    args.put("me", currentUser.username());
    args.put("all", includeDone);
    args.put("size", size);
    args.put("offset", (long) page * size);
    LocalDate today = ClaimAgeing.today(clock);
    List<DiaryItem> rows = jdbc.query(SELECT, args, (rs, n) -> item(rs, today));
    Long total = jdbc.queryForObject("select count(*)" + FROM, args, Long.class);
    long count = total == null ? 0 : total;
    int pages = size == 0 ? 0 : (int) ((count + size - 1) / size);
    return new PageResponse<>(rows, page, size, count, pages);
  }

  private static DiaryItem item(ResultSet rs, LocalDate today) throws SQLException {
    LocalDate due = date(rs.getDate("due_date"));
    Instant doneAt = instant(rs.getTimestamp("done_at"));
    return new DiaryItem(
        rs.getLong("id"),
        rs.getLong("claim_id"),
        rs.getString("claim_no"),
        rs.getString("assured_name"),
        rs.getString("entry_type"),
        rs.getString("type_label"),
        date(rs.getDate("entry_date")),
        due,
        rs.getString("assignee"),
        rs.getString("text"),
        doneAt,
        rs.getString("done_by"),
        rs.getString("created_by"),
        doneAt == null && due != null && due.isBefore(today));
  }

  private static LocalDate date(Date value) {
    return value == null ? null : value.toLocalDate();
  }

  private static Instant instant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  /**
   * A diary entry of My Diary.
   *
   * @param id entry
   * @param claimId claim
   * @param claimNo claim number
   * @param assuredName assured
   * @param entryType type
   * @param typeLabel type label
   * @param entryDate date
   * @param dueDate due date
   * @param assignee assignee
   * @param text text
   * @param doneAt completion time
   * @param doneBy completed by
   * @param createdBy author
   * @param overdue open and past its due date
   */
  public record DiaryItem(
      Long id,
      Long claimId,
      String claimNo,
      String assuredName,
      String entryType,
      String typeLabel,
      LocalDate entryDate,
      LocalDate dueDate,
      String assignee,
      String text,
      Instant doneAt,
      String doneBy,
      String createdBy,
      boolean overdue) {}
}
