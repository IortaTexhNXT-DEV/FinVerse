package com.iortatechnxt.brokerverse.nbadmin.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The users, their group profiles and the access change log, read for the user access reports (BRD
 * 3.003.1, 3.003.3; UQ11): the profiles a user held at the end of an as-of date are the current
 * ones, undone by the later changes of the log; "created / modified by" is the approver of the
 * request that applied the change (its request number), or the person who made a direct change.
 */
@Component
class UserAccessHistory {

  /** Attribute of the group profiles in the change log. */
  static final String ROLES = "roles";

  private static final String ENABLED = "enabled";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the reader.
   *
   * @param jdbc JDBC
   */
  UserAccessHistory(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Reads everything the reports need (fewer than a few hundred users; UAM-NFR-01).
   *
   * @return snapshot
   */
  Snapshot load() {
    Map<String, Set<String>> roles = new HashMap<>();
    jdbc.query(
        "select u.username, r.code from sec_user_role ur join sec_user u on u.id = ur.user_id"
            + " join sec_role r on r.id = ur.role_id",
        rs -> {
          roles.computeIfAbsent(key(rs.getString(1)), k -> new TreeSet<>()).add(rs.getString(2));
        });
    List<UserRow> users =
        jdbc.query(
            "select username, full_name, windows_id, business_unit_code, user_level, enabled,"
                + " locked, created_at, created_by from sec_user order by lower(username)",
            (rs, n) ->
                new UserRow(
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("windows_id"),
                    rs.getString("business_unit_code"),
                    rs.getString("user_level"),
                    rs.getBoolean(ENABLED),
                    rs.getBoolean("locked"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getString("created_by"),
                    roles.getOrDefault(key(rs.getString("username")), Set.of())));
    Map<String, String> roleNames = new LinkedHashMap<>();
    jdbc.query(
        "select code, name from sec_role order by code",
        rs -> {
          roleNames.put(rs.getString(1), rs.getString(2));
        });
    Map<String, List<Change>> changes = new HashMap<>();
    jdbc.query(
        "select subject, occurred_at, activity, attribute, from_value, to_value, request_no,"
            + " done_by, approved_by from sec_access_change_log where subject_type = 'USER'"
            + " order by occurred_at, id",
        rs -> {
          changes
              .computeIfAbsent(key(rs.getString("subject")), k -> new ArrayList<>())
              .add(
                  new Change(
                      rs.getTimestamp("occurred_at").toInstant(),
                      rs.getString("activity"),
                      rs.getString("attribute"),
                      rs.getString("from_value"),
                      rs.getString("to_value"),
                      rs.getString("request_no"),
                      rs.getString("done_by"),
                      rs.getString("approved_by")));
        });
    return new Snapshot(users, roleNames, changes);
  }

  private static String key(String username) {
    return username == null ? "" : username.toLowerCase(Locale.ROOT);
  }

  /**
   * Codes of a comma separated list.
   *
   * @param codes list, may be null
   * @return codes
   */
  static Set<String> codes(String codes) {
    if (codes == null || codes.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(codes.split(","))
        .map(String::trim)
        .filter(c -> !c.isEmpty())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  /**
   * What the reports read.
   *
   * @param users users, by user name
   * @param roleNames group profile name by code
   * @param changes change log rows of each user (lower-case user name), oldest first
   */
  record Snapshot(
      List<UserRow> users, Map<String, String> roleNames, Map<String, List<Change>> changes) {

    /**
     * Change log rows of a user, oldest first.
     *
     * @param user user
     * @return rows
     */
    List<Change> of(UserRow user) {
      return changes.getOrDefault(key(user.username()), List.of());
    }

    /**
     * The group profiles a user held just before a time.
     *
     * @param user user
     * @param end the first instant after the as-of date
     * @return role codes
     */
    Set<String> rolesAt(UserRow user, Instant end) {
      return firstChangeFrom(user, ROLES, end).map(c -> codes(c.from())).orElse(user.roles());
    }

    /**
     * Whether a user was enabled just before a time.
     *
     * @param user user
     * @param end the first instant after the as-of date
     * @return true when enabled
     */
    boolean enabledAt(UserRow user, Instant end) {
      return firstChangeFrom(user, ENABLED, end)
          .map(c -> Boolean.parseBoolean(c.from()))
          .orElse(user.enabled());
    }

    /**
     * The changes of a user before a time.
     *
     * @param user user
     * @param end the first instant after the as-of date
     * @return rows, oldest first
     */
    List<Change> before(UserRow user, Instant end) {
      return of(user).stream().filter(c -> c.at().isBefore(end)).toList();
    }

    /**
     * The change that last gave a user a group profile before a time.
     *
     * @param user user
     * @param role role code
     * @param end the first instant after the as-of date
     * @return the change, empty when the profile was held from the start (no log)
     */
    Optional<Change> addition(UserRow user, String role, Instant end) {
      Change found = null;
      for (Change c : before(user, end)) {
        if (ROLES.equals(c.attribute())
            && codes(c.to()).contains(role)
            && !codes(c.from()).contains(role)) {
          found = c;
        }
      }
      return Optional.ofNullable(found);
    }

    private Optional<Change> firstChangeFrom(UserRow user, String attribute, Instant end) {
      return of(user).stream()
          .filter(c -> attribute.equals(c.attribute()) && !c.at().isBefore(end))
          .findFirst();
    }
  }

  /**
   * A user.
   *
   * @param username user ID
   * @param fullName full name
   * @param windowsId Windows ID
   * @param businessUnit business unit group
   * @param userLevel user level
   * @param enabled enabled now
   * @param locked locked now
   * @param createdAt creation
   * @param createdBy creator (session)
   * @param roles group profiles now
   */
  record UserRow(
      String username,
      String fullName,
      String windowsId,
      String businessUnit,
      String userLevel,
      boolean enabled,
      boolean locked,
      Instant createdAt,
      String createdBy,
      Set<String> roles) {}

  /**
   * A row of the access change log.
   *
   * @param at time
   * @param activity activity
   * @param attribute attribute
   * @param from value before
   * @param to value after
   * @param requestNo request number
   * @param doneBy actor
   * @param approvedBy approver of the request
   */
  record Change(
      Instant at,
      String activity,
      String attribute,
      String from,
      String to,
      String requestNo,
      String doneBy,
      String approvedBy) {

    /**
     * Who is shown as having made the change: the approver and request number of a request, or the
     * person who made a direct change.
     *
     * @return actor text
     */
    String actor() {
      if (approvedBy == null) {
        return doneBy;
      }
      return requestNo == null ? approvedBy : approvedBy + " (" + requestNo + ")";
    }
  }
}
