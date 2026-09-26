package com.iortatechnxt.brokerverse.brokerclaims.home.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Claims home (NFR 15.03, BRCLM.025/034; FR-CL-055): the work tiles of the signed-in user (my open
 * claims, follow-ups due today and overdue, my diary due, temporarily closed, unpaid premium,
 * awaiting premium remittance), the open claims by status and phase and the ageing buckets of the
 * outstanding claims ({@code BCL_AGEING_BUCKETS}). Each tile opens the worklist filtered.
 */
@Service
@Transactional(readOnly = true)
public class ClaimsHomeService {

  private static final String WORKLIST = "/claims-handling/worklist?";

  private static final String COUNTS =
      "select"
          + " count(*) filter (where c.phase <> 'CLOSED' and lower(c.handler) = lower(:me)) as mine,"
          + " count(*) filter (where c.phase <> 'CLOSED' and c.next_follow_up_date = :today)"
          + " as due_today,"
          + " count(*) filter (where c.phase <> 'CLOSED' and c.next_follow_up_date < :today)"
          + " as overdue,"
          + " count(*) filter (where c.phase = 'TEMP_CLOSED') as temp_closed,"
          + " count(*) filter (where c.phase <> 'CLOSED'"
          + " and c.premium_status in ('UNPAID', 'PARTIALLY_PAID')) as unpaid,"
          + " count(*) filter (where c.phase <> 'CLOSED' and exists (select 1 from"
          + " bcl_lov_attribute a where a.type_code = 'BCL_CLAIM_STATUS' and a.code = c.status_code"
          + " and a.attribute = 'awaiting_premium_remittance' and lower(a.value) = 'true'))"
          + " as awaiting,"
          + " (select count(*) from bcl_diary_entry d join bcl_claim x on x.id = d.claim_id"
          + " where x.company_id = :companyId and lower(d.assignee) = lower(:me)"
          + " and d.done_at is null and d.due_date <= :today) as diary_due"
          + " from bcl_claim c where c.company_id = :companyId";

  private static final String BY_STATUS =
      "select c.phase, c.status_code, coalesce(s.label, c.status_code) as status_label,"
          + " count(*) as claims from bcl_claim c"
          + " left join lov_value s on s.type_code = 'BCL_CLAIM_STATUS' and s.code = c.status_code"
          + " where c.company_id = :companyId and c.phase <> 'CLOSED'"
          + " group by c.phase, c.status_code, s.label, s.sort_order"
          + " order by c.phase, s.sort_order, c.status_code";

  private static final String AGES =
      "select (cast(:today as date) - c.reported_date) as age, count(*) as claims"
          + " from bcl_claim c where c.company_id = :companyId and c.phase <> 'CLOSED'"
          + " group by 1";

  private final NamedParameterJdbcTemplate jdbc;
  private final SystemParameterService parameters;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param jdbc named-parameter JDBC
   * @param parameters business parameters (ageing buckets)
   * @param currentUser current user
   * @param clock clock
   */
  public ClaimsHomeService(
      NamedParameterJdbcTemplate jdbc,
      SystemParameterService parameters,
      CurrentUser currentUser,
      Clock clock) {
    this.jdbc = jdbc;
    this.parameters = parameters;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The home of the signed-in user.
   *
   * @param companyId company
   * @return tiles, open claims by status and ageing buckets
   */
  public Home home(Long companyId) {
    LocalDate today = ClaimAgeing.today(clock);
    Map<String, Object> args =
        Map.of("companyId", companyId, "me", currentUser.username(), "today", today);
    List<Tile> tiles = tiles(jdbc.queryForMap(COUNTS, args));
    List<StatusCount> byStatus =
        jdbc.query(
            BY_STATUS,
            args,
            (rs, n) ->
                new StatusCount(
                    rs.getString("phase"),
                    rs.getString("status_code"),
                    rs.getString("status_label"),
                    rs.getLong("claims")));
    return new Home(tiles, byStatus, ageing(args));
  }

  private static List<Tile> tiles(Map<String, Object> c) {
    return List.of(
        new Tile("mine", "My Open Claims", count(c, "mine"), false, WORKLIST + "tab=MINE"),
        new Tile(
            "dueToday",
            "Follow-ups Due Today",
            count(c, "due_today"),
            false,
            WORKLIST + "tab=FOLLOW_UPS_DUE"),
        new Tile(
            "overdue",
            "Follow-ups Overdue",
            count(c, "overdue"),
            true,
            WORKLIST + "tab=ALL&flag=OVERDUE"),
        new Tile("diary", "My Diary Due", count(c, "diary_due"), true, "/claims-handling/diary"),
        new Tile(
            "tempClosed",
            "Temporarily Closed",
            count(c, "temp_closed"),
            false,
            WORKLIST + "tab=TEMP_CLOSED"),
        new Tile(
            "unpaid",
            "Unpaid Premium",
            count(c, "unpaid"),
            true,
            WORKLIST + "tab=ALL&flag=UNPAID_PREMIUM"),
        new Tile(
            "awaiting",
            "Awaiting Premium Remittance",
            count(c, "awaiting"),
            true,
            WORKLIST + "tab=ALL&flag=AWAITING_REMITTANCE"));
  }

  private List<BucketCount> ageing(Map<String, Object> args) {
    List<Integer> bounds = bounds();
    Map<String, Long> counts = new LinkedHashMap<>();
    ClaimAgeing.buckets(bounds).forEach(b -> counts.put(b, 0L));
    jdbc.query(
        AGES,
        args,
        rs -> {
          String bucket = ClaimAgeing.bucket(rs.getInt("age"), bounds);
          counts.merge(bucket, rs.getLong("claims"), Long::sum);
        });
    List<BucketCount> result = new ArrayList<>();
    counts.forEach((bucket, claims) -> result.add(new BucketCount(bucket, claims)));
    return result;
  }

  /**
   * The upper bounds of the ageing buckets ({@code BCL_AGEING_BUCKETS}).
   *
   * @return increasing bounds
   */
  public List<Integer> bounds() {
    return parameters.items(ClaimCodes.PARAM_AGEING_BUCKETS).stream()
        .map(String::strip)
        .filter(s -> s.matches("\\d+"))
        .map(Integer::valueOf)
        .sorted()
        .toList();
  }

  private static long count(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value instanceof Number n ? n.longValue() : 0;
  }

  /**
   * The Claims home.
   *
   * @param tiles work tiles
   * @param byStatus open claims by phase and status
   * @param ageing outstanding claims by ageing bucket
   */
  public record Home(List<Tile> tiles, List<StatusCount> byStatus, List<BucketCount> ageing) {}

  /**
   * A work tile.
   *
   * @param key key
   * @param label label
   * @param value count
   * @param alert whether a non-zero count needs attention
   * @param link worklist route with its filter
   */
  public record Tile(String key, String label, long value, boolean alert, String link) {}

  /**
   * Open claims of a status.
   *
   * @param phase phase
   * @param statusCode status
   * @param statusLabel label
   * @param claims count
   */
  public record StatusCount(String phase, String statusCode, String statusLabel, long claims) {}

  /**
   * Outstanding claims of an ageing bucket.
   *
   * @param bucket bucket label, e.g. 31-60
   * @param claims count
   */
  public record BucketCount(String bucket, long claims) {}
}
