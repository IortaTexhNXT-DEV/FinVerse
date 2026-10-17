package com.iortatechnxt.brokerverse.csf.cases.domain;

import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.AccountRef;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Category;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Escalation;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Handling;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Party;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Sla;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * The cases of the contact centre and their notes ({@code csf_case}, {@code csf_case_note}).
 * Updates check the version of the case, so two users acting on the same case at once do not
 * overwrite each other.
 */
@Repository
public class CsfCaseStore {

  private static final String COLUMNS =
      "id, version, company_id, case_no, client_id, client_code, client_name, caller_name,"
          + " caller_contact, account_id, arn, policy_no, channel, case_type, nature, product_type,"
          + " market_segment, fulfilment_unit, priority, subject, description, status, owner, agent,"
          + " tat_days, due_date, sla_status, near_notified, breach_notified, escalated,"
          + " escalation_reason, escalated_at, return_reason, instruction, resolution,"
          + " closure_reason, closed_at, closed_by, parent_case_id, follow_up_reason, created_at";

  private static final String INSERT =
      "insert into csf_case (company_id, case_no, client_id, client_code, client_name, caller_name,"
          + " caller_contact, account_id, arn, policy_no, channel, case_type, nature, product_type,"
          + " market_segment, fulfilment_unit, priority, subject, description, status, owner,"
          + " agent, tat_days, due_date, sla_status, resolution, closure_reason, closed_at,"
          + " closed_by, parent_case_id, follow_up_reason, created_at, created_by) values"
          + " (:company, :caseNo, :clientId, :clientCode, :clientName, :callerName,"
          + " :callerContact, :accountId, :arn, :policyNo, :channel, :caseType, :nature,"
          + " :productType, :marketSegment, :unit, :priority, :subject, :description, :status,"
          + " :owner, :agent, :tatDays, :dueDate, :sla, :resolution, :closureReason, :closedAt,"
          + " :closedBy, :parentCaseId, :followUpReason, :createdAt, :agent)";

  private static final String UPDATE =
      "update csf_case set version = version + 1, case_type = :caseType, nature = :nature,"
          + " fulfilment_unit = :unit, priority = :priority, status = :status, owner = :owner,"
          + " tat_days = :tatDays, due_date = :dueDate, sla_status = :sla,"
          + " near_notified = :nearNotified, breach_notified = :breachNotified,"
          + " escalated = :escalated, escalation_reason = :escalationReason,"
          + " escalated_at = :escalatedAt, return_reason = :returnReason,"
          + " instruction = :instruction, resolution = :resolution,"
          + " closure_reason = :closureReason, closed_at = :closedAt, closed_by = :closedBy,"
          + " updated_at = :now, updated_by = :user where id = :id and version = :version";

  private static final String SELECT = "select " + COLUMNS;

  private static final String LIST =
      SELECT
          + " from csf_case where company_id = :company and status in (:statuses)"
          + " and (cast(:owner as text) is null or lower(owner) = lower(cast(:owner as text)))"
          + " and (cast(:from as date) is null or cast(created_at as date) >= :from)"
          + " and (cast(:to as date) is null or cast(created_at as date) <= :to)"
          + " and (:allUnits or fulfilment_unit in (:units)) and (not :escalatedOnly or escalated)"
          + " order by created_at desc, id desc limit :limit";

  private static final String COMPANY = "company";
  private static final String NATURE = "nature";
  private static final String OWNER = "owner";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the store.
   *
   * @param jdbc JDBC
   */
  public CsfCaseStore(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * A filter of the case lists. Null values are not filtered on.
   *
   * @param companyId company
   * @param statuses statuses to include
   * @param owner owner (user ID)
   * @param units fulfilment units the case must be in
   * @param from first creation day
   * @param to last creation day
   * @param escalatedOnly only escalated cases
   */
  public record Filter(
      Long companyId,
      Collection<CaseStatus> statuses,
      String owner,
      Collection<String> units,
      LocalDate from,
      LocalDate to,
      boolean escalatedOnly) {}

  /**
   * One note of a case.
   *
   * @param id id
   * @param note text
   * @param author user
   * @param createdAt time
   */
  public record Note(Long id, String note, String author, Instant createdAt) {}

  /**
   * Saves a new case.
   *
   * @param c case (id and version are ignored)
   * @return id
   */
  public Long insert(CsfCase c) {
    MapSqlParameterSource p =
        new MapSqlParameterSource()
            .addValue(COMPANY, c.companyId())
            .addValue("caseNo", c.caseNo())
            .addValue("clientId", c.party().clientId())
            .addValue("clientCode", c.party().clientCode())
            .addValue("clientName", c.party().clientName())
            .addValue("callerName", c.party().callerName())
            .addValue("callerContact", c.party().callerContact())
            .addValue("accountId", c.account().accountId())
            .addValue("arn", c.account().arn())
            .addValue("policyNo", c.account().policyNo())
            .addValue("channel", c.category().channel())
            .addValue("caseType", c.category().caseType())
            .addValue(NATURE, c.category().nature())
            .addValue("productType", c.category().productType())
            .addValue("marketSegment", c.category().marketSegment())
            .addValue("unit", c.category().fulfilmentUnit())
            .addValue("priority", c.category().priority())
            .addValue("subject", c.subject())
            .addValue("description", c.description())
            .addValue("status", c.status().name())
            .addValue(OWNER, c.owner())
            .addValue("agent", c.agent())
            .addValue("tatDays", c.tatDays())
            .addValue("dueDate", Date.valueOf(c.dueDate()))
            .addValue("sla", c.sla().status())
            .addValue("resolution", c.handling().resolution())
            .addValue("closureReason", c.handling().closureReason())
            .addValue("closedAt", timestamp(c.handling().closedAt()))
            .addValue("closedBy", c.handling().closedBy())
            .addValue("parentCaseId", c.parentCaseId())
            .addValue("followUpReason", c.followUpReason())
            .addValue("createdAt", timestamp(c.createdAt()));
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(INSERT, p, keys, new String[] {"id"});
    Number id = keys.getKey();
    if (id == null) {
      throw new IllegalStateException("No id returned for case " + c.caseNo());
    }
    return id.longValue();
  }

  /**
   * Saves the changes of a case.
   *
   * @param c case as changed
   * @param user acting user
   * @param now time of the change
   * @throws OptimisticLockingFailureException when the case was changed in the meantime
   */
  public void update(CsfCase c, String user, Instant now) {
    MapSqlParameterSource p =
        new MapSqlParameterSource()
            .addValue("id", c.id())
            .addValue("version", c.version())
            .addValue("caseType", c.category().caseType())
            .addValue(NATURE, c.category().nature())
            .addValue("unit", c.category().fulfilmentUnit())
            .addValue("priority", c.category().priority())
            .addValue("status", c.status().name())
            .addValue(OWNER, c.owner())
            .addValue("tatDays", c.tatDays())
            .addValue("dueDate", Date.valueOf(c.dueDate()))
            .addValue("sla", c.sla().status())
            .addValue("nearNotified", c.sla().nearNotified())
            .addValue("breachNotified", c.sla().breachNotified())
            .addValue("escalated", c.escalation().escalated())
            .addValue("escalationReason", c.escalation().reason())
            .addValue("escalatedAt", timestamp(c.escalation().at()))
            .addValue("returnReason", c.handling().returnReason())
            .addValue("instruction", c.handling().instruction())
            .addValue("resolution", c.handling().resolution())
            .addValue("closureReason", c.handling().closureReason())
            .addValue("closedAt", timestamp(c.handling().closedAt()))
            .addValue("closedBy", c.handling().closedBy())
            .addValue("now", timestamp(now))
            .addValue("user", user);
    if (jdbc.update(UPDATE, p) != 1) {
      throw new OptimisticLockingFailureException(
          "Case " + c.caseNo() + " was changed by another user. Open it again");
    }
  }

  /**
   * A case.
   *
   * @param id id
   * @return case
   */
  public Optional<CsfCase> find(Long id) {
    return jdbc
        .query(SELECT + " from csf_case where id = :id", Map.of("id", id), (rs, n) -> row(rs))
        .stream()
        .findFirst();
  }

  /**
   * The open case of the same transaction: client, account and nature of transaction.
   *
   * @param companyId company
   * @param clientId client
   * @param accountId account, may be null
   * @param nature nature of transaction
   * @return Case ID of the open case
   */
  public Optional<CsfCase> openDuplicate(
      Long companyId, Long clientId, Long accountId, String nature) {
    Map<String, Object> p = new HashMap<>();
    p.put(COMPANY, companyId);
    p.put("client", clientId);
    p.put("account", accountId);
    p.put(NATURE, nature);
    p.put("statuses", names(CaseStatus.open()));
    return jdbc
        .query(
            SELECT
                + " from csf_case where company_id = :company and client_id = :client"
                + " and coalesce(account_id, 0) = coalesce(cast(:account as bigint), 0)"
                + " and nature = :nature and status in (:statuses) order by id limit 1",
            p,
            (rs, n) -> row(rs))
        .stream()
        .findFirst();
  }

  /**
   * The cases of a filter, newest first.
   *
   * @param f filter
   * @param limit maximum rows
   * @return cases
   */
  public List<CsfCase> list(Filter f, int limit) {
    Map<String, Object> p = new HashMap<>();
    p.put(COMPANY, f.companyId());
    p.put("statuses", names(f.statuses()));
    p.put(OWNER, f.owner());
    p.put("from", f.from() == null ? null : Date.valueOf(f.from()));
    p.put("to", f.to() == null ? null : Date.valueOf(f.to()));
    p.put("limit", limit);
    if (f.units() != null && f.units().isEmpty()) {
      return List.of();
    }
    p.put("allUnits", f.units() == null);
    p.put("units", f.units() == null ? List.of("") : f.units());
    p.put("escalatedOnly", f.escalatedOnly());
    return jdbc.query(LIST, p, (rs, n) -> row(rs));
  }

  /**
   * The cases of a client, newest first.
   *
   * @param companyId company
   * @param clientId client
   * @return cases
   */
  public List<CsfCase> ofClient(Long companyId, Long clientId) {
    return jdbc.query(
        SELECT
            + " from csf_case where company_id = :company and client_id = :client"
            + " order by created_at desc, id desc",
        Map.of(COMPANY, companyId, "client", clientId),
        (rs, n) -> row(rs));
  }

  /**
   * The follow-up cases created from a case.
   *
   * @param caseId original case
   * @return cases
   */
  public List<CsfCase> followUps(Long caseId) {
    return jdbc.query(
        SELECT + " from csf_case where parent_case_id = :id order by id",
        Map.of("id", caseId),
        (rs, n) -> row(rs));
  }

  /**
   * The open cases of every company, for the SLA check.
   *
   * @return cases
   */
  public List<CsfCase> allOpen() {
    return jdbc.query(
        SELECT + " from csf_case where status in (:statuses) order by id",
        Map.of("statuses", names(CaseStatus.open())),
        (rs, n) -> row(rs));
  }

  /**
   * Adds a note.
   *
   * @param caseId case
   * @param note text
   * @param author user
   * @param at time
   */
  public void addNote(Long caseId, String note, String author, Instant at) {
    jdbc.update(
        "insert into csf_case_note (case_id, note, author, created_at)"
            + " values (:id, :note, :author, :at)",
        Map.of("id", caseId, "note", note, "author", author, "at", timestamp(at)));
  }

  /**
   * The notes of a case, oldest first.
   *
   * @param caseId case
   * @return notes
   */
  public List<Note> notes(Long caseId) {
    return jdbc.query(
        "select id, note, author, created_at from csf_case_note where case_id = :id"
            + " order by created_at, id",
        Map.of("id", caseId),
        (rs, n) ->
            new Note(
                rs.getLong("id"),
                rs.getString("note"),
                rs.getString("author"),
                instant(rs.getTimestamp("created_at"))));
  }

  private static List<String> names(Collection<CaseStatus> statuses) {
    return statuses.stream().map(Enum::name).toList();
  }

  private static Timestamp timestamp(Instant i) {
    return i == null ? null : Timestamp.from(i);
  }

  private static Instant instant(Timestamp t) {
    return t == null ? null : t.toInstant();
  }

  private static Long nullableLong(ResultSet rs, String column) throws SQLException {
    long v = rs.getLong(column);
    return rs.wasNull() ? null : v;
  }

  private static CsfCase row(ResultSet rs) throws SQLException {
    return new CsfCase(
        rs.getLong("id"),
        rs.getLong("version"),
        rs.getLong("company_id"),
        rs.getString("case_no"),
        new Party(
            nullableLong(rs, "client_id"),
            rs.getString("client_code"),
            rs.getString("client_name"),
            rs.getString("caller_name"),
            rs.getString("caller_contact")),
        new AccountRef(
            nullableLong(rs, "account_id"), rs.getString("arn"), rs.getString("policy_no")),
        new Category(
            rs.getString("channel"),
            rs.getString("case_type"),
            rs.getString(NATURE),
            rs.getString("product_type"),
            rs.getString("market_segment"),
            rs.getString("fulfilment_unit"),
            rs.getString("priority")),
        rs.getString("subject"),
        rs.getString("description"),
        CaseStatus.valueOf(rs.getString("status")),
        rs.getString(OWNER),
        rs.getString("agent"),
        rs.getInt("tat_days"),
        rs.getDate("due_date").toLocalDate(),
        new Sla(
            rs.getString("sla_status"),
            rs.getBoolean("near_notified"),
            rs.getBoolean("breach_notified")),
        new Escalation(
            rs.getBoolean("escalated"),
            rs.getString("escalation_reason"),
            instant(rs.getTimestamp("escalated_at"))),
        new Handling(
            rs.getString("return_reason"),
            rs.getString("instruction"),
            rs.getString("resolution"),
            rs.getString("closure_reason"),
            instant(rs.getTimestamp("closed_at")),
            rs.getString("closed_by")),
        nullableLong(rs, "parent_case_id"),
        rs.getString("follow_up_reason"),
        instant(rs.getTimestamp("created_at")));
  }
}
