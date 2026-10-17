package com.iortatechnxt.brokerverse.csf.cases.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The configuration of case management: the turnaround time of each case type ({@code
 * csf_case_type_setting}), the routing table ({@code csf_case_routing}) and the members of the
 * fulfilment units ({@code csf_fulfilment_member}).
 */
@Repository
public class CsfCaseSetupStore {

  private static final String USER = "user";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the store.
   *
   * @param jdbc JDBC
   */
  public CsfCaseSetupStore(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The turnaround time of a case type.
   *
   * @param caseType case type code
   * @param tatDays working days
   * @param autoEscalate escalate automatically on breach
   */
  public record TypeSetting(String caseType, int tatDays, boolean autoEscalate) {}

  /**
   * A row of the routing table; an empty text matches any value.
   *
   * @param id id
   * @param caseType case type or empty
   * @param marketSegment market segment or empty
   * @param nature nature of transaction or empty
   * @param fulfilmentUnit fulfilment unit
   * @param escalationGroup fulfilment unit of the escalations, null for the supervisors
   * @param active active
   */
  public record Route(
      Long id,
      String caseType,
      String marketSegment,
      String nature,
      String fulfilmentUnit,
      String escalationGroup,
      boolean active) {}

  /**
   * A member of a fulfilment unit.
   *
   * @param id id
   * @param unitCode fulfilment unit
   * @param username user ID
   */
  public record Member(Long id, String unitCode, String username) {}

  /**
   * The turnaround times of the case types.
   *
   * @return settings by case type
   */
  public List<TypeSetting> typeSettings() {
    return jdbc.query(
        "select case_type, tat_days, auto_escalate from csf_case_type_setting order by case_type",
        Map.of(),
        (rs, n) ->
            new TypeSetting(
                rs.getString("case_type"), rs.getInt("tat_days"), rs.getBoolean("auto_escalate")));
  }

  /**
   * The turnaround time of one case type.
   *
   * @param caseType case type
   * @return setting
   */
  public Optional<TypeSetting> typeSetting(String caseType) {
    return typeSettings().stream().filter(t -> t.caseType().equals(caseType)).findFirst();
  }

  /**
   * Saves the turnaround time of a case type.
   *
   * @param s setting
   * @param user acting user
   */
  public void saveTypeSetting(TypeSetting s, String user) {
    jdbc.update(
        "insert into csf_case_type_setting (case_type, tat_days, auto_escalate, created_at,"
            + " created_by) values (:type, :tat, :auto, now(), :user) on conflict (case_type) do"
            + " update set tat_days = :tat, auto_escalate = :auto, version ="
            + " csf_case_type_setting.version + 1, updated_at = now(), updated_by = :user",
        Map.of("type", s.caseType(), "tat", s.tatDays(), "auto", s.autoEscalate(), USER, user));
  }

  /**
   * The routing table.
   *
   * @return rows
   */
  public List<Route> routes() {
    return jdbc.query(
        "select id, case_type, market_segment, nature, fulfilment_unit, escalation_group, active"
            + " from csf_case_routing order by case_type, market_segment, nature",
        Map.of(),
        (rs, n) ->
            new Route(
                rs.getLong("id"),
                rs.getString("case_type"),
                rs.getString("market_segment"),
                rs.getString("nature"),
                rs.getString("fulfilment_unit"),
                rs.getString("escalation_group"),
                rs.getBoolean("active")));
  }

  /**
   * Whether a routing row exists for the keys.
   *
   * @param caseType case type or empty
   * @param segment market segment or empty
   * @param nature nature or empty
   * @param exceptId row to leave out, may be null
   * @return true when one exists
   */
  public boolean routeExists(String caseType, String segment, String nature, Long exceptId) {
    Map<String, Object> p = new HashMap<>();
    p.put("type", caseType);
    p.put("segment", segment);
    p.put("nature", nature);
    p.put("id", exceptId == null ? Long.valueOf(-1L) : exceptId);
    Integer n =
        jdbc.queryForObject(
            "select count(*) from csf_case_routing where case_type = :type and market_segment ="
                + " :segment and nature = :nature and id <> :id",
            p,
            Integer.class);
    return n != null && n > 0;
  }

  /**
   * Adds or changes a routing row.
   *
   * @param r row (id null for a new row)
   * @param user acting user
   */
  public void saveRoute(Route r, String user) {
    Map<String, Object> p = new HashMap<>();
    p.put("id", r.id());
    p.put("type", r.caseType());
    p.put("segment", r.marketSegment());
    p.put("nature", r.nature());
    p.put("unit", r.fulfilmentUnit());
    p.put("group", r.escalationGroup());
    p.put("active", r.active());
    p.put(USER, user);
    if (r.id() == null) {
      jdbc.update(
          "insert into csf_case_routing (case_type, market_segment, nature, fulfilment_unit,"
              + " escalation_group, active, created_at, created_by) values (:type, :segment,"
              + " :nature, :unit, :group, :active, now(), :user)",
          p);
    } else {
      jdbc.update(
          "update csf_case_routing set case_type = :type, market_segment = :segment, nature ="
              + " :nature, fulfilment_unit = :unit, escalation_group = :group, active = :active,"
              + " version = version + 1, updated_at = now(), updated_by = :user where id = :id",
          p);
    }
  }

  /**
   * The members of the fulfilment units.
   *
   * @return members
   */
  public List<Member> members() {
    return jdbc.query(
        "select id, unit_code, username from csf_fulfilment_member order by unit_code, username",
        Map.of(),
        (rs, n) ->
            new Member(rs.getLong("id"), rs.getString("unit_code"), rs.getString("username")));
  }

  /**
   * The fulfilment units of a user.
   *
   * @param username user ID
   * @return unit codes
   */
  public List<String> unitsOf(String username) {
    return jdbc.queryForList(
        "select unit_code from csf_fulfilment_member where lower(username) = lower(:user)"
            + " order by unit_code",
        Map.of("user", username),
        String.class);
  }

  /**
   * The members of a fulfilment unit.
   *
   * @param unitCode unit
   * @return user IDs
   */
  public List<String> membersOf(String unitCode) {
    return jdbc.queryForList(
        "select username from csf_fulfilment_member where unit_code = :unit order by username",
        Map.of("unit", unitCode),
        String.class);
  }

  /**
   * Adds a member to a unit (no change when already a member).
   *
   * @param unitCode unit
   * @param username user ID
   * @param user acting user
   */
  public void addMember(String unitCode, String username, String user) {
    jdbc.update(
        "insert into csf_fulfilment_member (unit_code, username, created_at, created_by)"
            + " values (:unit, :member, now(), :user) on conflict (unit_code, username) do nothing",
        Map.of("unit", unitCode, "member", username, USER, user));
  }

  /**
   * Removes a member.
   *
   * @param id membership
   * @return removed rows
   */
  public int removeMember(Long id) {
    return jdbc.update("delete from csf_fulfilment_member where id = :id", Map.of("id", id));
  }
}
