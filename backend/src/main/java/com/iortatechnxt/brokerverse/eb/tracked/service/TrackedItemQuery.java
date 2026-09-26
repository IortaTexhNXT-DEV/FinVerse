package com.iortatechnxt.brokerverse.eb.tracked.service;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Pending Items list (BRID-030; FR-EB-057): tracked items of a company filtered by programme,
 * member, type, responsible party, status and overdue, searched by programme, client, subject or
 * ARN, oldest due first, with the days past due.
 */
@Service
@Transactional(readOnly = true)
public class TrackedItemQuery {

  private static final String WHERE =
      " from eb_tracked_item i join eb_programme p on p.id = i.programme_id"
          + " where i.company_id = :companyId"
          + " and (cast(:itemId as bigint) is null or i.id = :itemId)"
          + " and (cast(:programmeId as bigint) is null or i.programme_id = :programmeId)"
          + " and (cast(:member as varchar) is null or i.member_ref ilike :memberLike)"
          + " and (cast(:type as varchar) is null or i.item_type = :type)"
          + " and (cast(:responsible as varchar) is null or i.responsible = :responsible)"
          + " and (cast(:status as varchar) is null or i.status = :status)"
          + " and (:overdue = false or (i.status = 'PENDING' and i.due_date < :today))"
          + " and (cast(:q as varchar) is null or p.programme_no ilike :like"
          + " or p.client_name ilike :like or i.subject ilike :like or i.account_arn ilike :like)";

  private static final String SELECT =
      "select i.id, i.programme_id, p.programme_no, p.client_name, i.cycle_id, i.item_type,"
          + " i.subject, i.member_ref, i.member_change_ref, i.account_arn, i.responsible,"
          + " i.party_code, i.recipient_email, i.status, i.due_date, i.follow_ups_sent,"
          + " i.last_follow_up_at, i.escalated_at, i.received_on, i.released_on, i.closed_on,"
          + " i.remarks"
          + WHERE
          + " order by case when i.status = 'PENDING' then 0 else 1 end, i.due_date, i.id"
          + " limit :size offset :offset";

  private final NamedParameterJdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the query.
   *
   * @param jdbc named-parameter JDBC
   * @param clock clock
   */
  public TrackedItemQuery(NamedParameterJdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * A page of tracked items.
   *
   * @param companyId company
   * @param criteria filters
   * @param page zero-based page
   * @param size page size
   * @return items
   */
  public PageResponse<ItemRow> search(Long companyId, ItemCriteria criteria, int page, int size) {
    LocalDate today = BusinessClock.today(clock);
    Map<String, Object> args = args(companyId, criteria, today);
    args.put("size", size);
    args.put("offset", (long) page * size);
    List<ItemRow> rows = jdbc.query(SELECT, args, (rs, n) -> row(rs, today));
    Long total = jdbc.queryForObject("select count(*)" + WHERE, args, Long.class);
    long count = total == null ? 0 : total;
    return new PageResponse<>(rows, page, size, count, (int) ((count + size - 1) / size));
  }

  /**
   * One item as listed.
   *
   * @param companyId company
   * @param itemId item
   * @return the row
   */
  public ItemRow row(Long companyId, Long itemId) {
    LocalDate today = BusinessClock.today(clock);
    Map<String, Object> args =
        args(companyId, new ItemCriteria(null, null, null, null, null, false, null), today);
    args.put("itemId", itemId);
    args.put("size", 1);
    args.put("offset", 0L);
    return jdbc.query(SELECT, args, (rs, n) -> row(rs, today)).stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_TRACKED_ITEM, itemId));
  }

  /**
   * Number of pending items past due (EB Home tile).
   *
   * @param companyId company
   * @return count
   */
  public long overdue(Long companyId) {
    LocalDate today = BusinessClock.today(clock);
    Long total =
        jdbc.queryForObject(
            "select count(*)" + WHERE,
            args(companyId, new ItemCriteria(null, null, null, null, null, true, null), today),
            Long.class);
    return total == null ? 0 : total;
  }

  private static Map<String, Object> args(Long companyId, ItemCriteria criteria, LocalDate today) {
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", companyId);
    args.put("today", today);
    args.put("itemId", null);
    args.put("programmeId", criteria.programmeId());
    String member = blankToNull(criteria.member());
    args.put("member", member);
    args.put("memberLike", member == null ? "" : "%" + escape(member) + "%");
    args.put("type", blankToNull(criteria.itemType()));
    args.put("responsible", blankToNull(criteria.responsible()));
    args.put("status", blankToNull(criteria.status()));
    args.put("overdue", criteria.overdue());
    String q = blankToNull(criteria.text());
    args.put("q", q);
    args.put("like", q == null ? "" : "%" + escape(q) + "%");
    return args;
  }

  private static ItemRow row(ResultSet rs, LocalDate today) throws SQLException {
    LocalDate due = rs.getDate("due_date").toLocalDate();
    String status = rs.getString("status");
    long cycleId = rs.getLong("cycle_id");
    Long cycle = rs.wasNull() ? null : cycleId;
    int pastDue =
        "PENDING".equals(status) && today.isAfter(due)
            ? (int) ChronoUnit.DAYS.between(due, today)
            : 0;
    return new ItemRow(
        rs.getLong("id"),
        rs.getLong("programme_id"),
        rs.getString("programme_no"),
        rs.getString("client_name"),
        cycle,
        rs.getString("item_type"),
        rs.getString("subject"),
        rs.getString("member_ref"),
        rs.getString("member_change_ref"),
        rs.getString("account_arn"),
        rs.getString("responsible"),
        rs.getString("party_code"),
        rs.getString("recipient_email"),
        status,
        due,
        pastDue,
        rs.getInt("follow_ups_sent"),
        instant(rs.getTimestamp("last_follow_up_at")),
        instant(rs.getTimestamp("escalated_at")),
        date(rs.getDate("received_on")),
        date(rs.getDate("released_on")),
        date(rs.getDate("closed_on")),
        rs.getString("remarks"));
  }

  private static Instant instant(Timestamp t) {
    return t == null ? null : t.toInstant();
  }

  private static LocalDate date(Date d) {
    return d == null ? null : d.toLocalDate();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String escape(String text) {
    return text.toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }

  /**
   * Filters of the Pending Items list.
   *
   * @param programmeId programme, may be null
   * @param member employee number or member name, may be null
   * @param itemType type, may be null
   * @param responsible INSURER, CLIENT or BDOI, may be null
   * @param status status, may be null
   * @param overdue only pending items past due
   * @param text programme number, client, subject or ARN, may be null
   */
  public record ItemCriteria(
      Long programmeId,
      String member,
      String itemType,
      String responsible,
      String status,
      boolean overdue,
      String text) {}

  /**
   * A tracked item as listed.
   *
   * @param id item
   * @param programmeId programme
   * @param programmeNo programme number
   * @param clientName client
   * @param cycleId cycle, may be null
   * @param itemType type
   * @param subject what is expected
   * @param memberRef member, may be null
   * @param memberChangeRef member change, may be null
   * @param accountArn account, may be null
   * @param responsible who owes it
   * @param partyCode insurer, may be null
   * @param recipientEmail follow-up recipients, may be null
   * @param status status
   * @param dueDate due date
   * @param daysPastDue days past due (pending items), else 0
   * @param followUpsSent follow-ups sent
   * @param lastFollowUpAt last follow-up
   * @param escalatedAt escalation
   * @param receivedOn date received
   * @param releasedOn date released
   * @param closedOn date closed
   * @param remarks remarks
   */
  public record ItemRow(
      Long id,
      Long programmeId,
      String programmeNo,
      String clientName,
      Long cycleId,
      String itemType,
      String subject,
      String memberRef,
      String memberChangeRef,
      String accountArn,
      String responsible,
      String partyCode,
      String recipientEmail,
      String status,
      LocalDate dueDate,
      int daysPastDue,
      int followUpsSent,
      Instant lastFollowUpAt,
      Instant escalatedAt,
      LocalDate receivedOn,
      LocalDate releasedOn,
      LocalDate closedOn,
      String remarks) {}
}
