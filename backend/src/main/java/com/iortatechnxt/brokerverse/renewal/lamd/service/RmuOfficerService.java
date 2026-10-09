package com.iortatechnxt.brokerverse.renewal.lamd.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RMU Account Maintenance (FRRN.041.01): the Account Officer codes of the Remedial Management Unit.
 * A loan of the CBG loans list whose Account Officer is in the list has the loan status RMU and
 * routes its renewal account to RMU. An active code is listed once.
 */
@Service
@Transactional
public class RmuOfficerService {

  /** Audit entity type. */
  public static final String ENTITY = "RmuOfficer";

  private static final Long NONE = -1L;

  private static final String LIST =
      "select id, ao_code, ao_name, active, remarks, created_by, created_at, updated_by,"
          + " updated_at from rnw_rmu_officer where company_id = :companyId"
          + " order by active desc, ao_code";

  private final NamedParameterJdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param jdbc list
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RmuOfficerService(
      NamedParameterJdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The list, active codes first.
   *
   * @param companyId company
   * @return officers
   */
  @Transactional(readOnly = true)
  public List<Officer> list(Long companyId) {
    return jdbc.query(
        LIST,
        Map.of("companyId", companyId),
        (rs, i) ->
            new Officer(
                rs.getLong("id"),
                rs.getString("ao_code"),
                rs.getString("ao_name"),
                rs.getBoolean("active"),
                rs.getString("remarks"),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getString("updated_by"),
                rs.getTimestamp("updated_at") == null
                    ? null
                    : rs.getTimestamp("updated_at").toInstant()));
  }

  /**
   * Adds an Account Officer code.
   *
   * @param companyId company
   * @param input code, name and remarks
   * @return the officer
   */
  public Officer add(Long companyId, Input input) {
    String code = require(input.aoCode(), "Enter the AO code");
    String name = require(input.aoName(), "Enter the Account Officer name");
    requireUnique(companyId, code, null);
    Map<String, Object> a = new HashMap<>();
    a.put("companyId", companyId);
    a.put("code", code);
    a.put("name", name);
    a.put("remarks", blank(input.remarks()));
    a.put("at", Timestamp.from(clock.instant()));
    a.put("by", currentUser.username());
    jdbc.update(
        "insert into rnw_rmu_officer (company_id, ao_code, ao_name, active, remarks, created_at,"
            + " created_by) values (:companyId, :code, :name, true, :remarks, :at, :by)",
        a);
    Officer o =
        list(companyId).stream()
            .filter(x -> x.active() && x.aoCode().equals(code))
            .findFirst()
            .orElseThrow();
    audit.record(ENTITY, code, AuditAction.CREATE, "RMU Account Officer " + code + " added");
    return o;
  }

  /**
   * Updates an Account Officer code: its name, remarks and whether it is active.
   *
   * @param companyId company
   * @param id officer
   * @param input name, remarks and active
   * @return the officer
   */
  public Officer update(Long companyId, Long id, Input input) {
    Officer old =
        list(companyId).stream()
            .filter(o -> o.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("RMU Account Officer", id));
    String name = require(input.aoName(), "Enter the Account Officer name");
    boolean active = input.active() == null || input.active();
    if (active && !old.active()) {
      requireUnique(companyId, old.aoCode(), id);
    }
    Map<String, Object> a = new HashMap<>();
    a.put("id", id);
    a.put("name", name);
    a.put("remarks", blank(input.remarks()));
    a.put("active", active);
    a.put("at", Timestamp.from(clock.instant()));
    a.put("by", currentUser.username());
    jdbc.update(
        "update rnw_rmu_officer set ao_name = :name, remarks = :remarks, active = :active,"
            + " updated_at = :at, updated_by = :by, version = version + 1 where id = :id",
        a);
    audit.record(
        ENTITY,
        old.aoCode(),
        AuditAction.UPDATE,
        "RMU Account Officer "
            + old.aoCode()
            + (old.active() == active ? " updated" : active ? " re-activated" : " deactivated"));
    return list(companyId).stream().filter(o -> o.id().equals(id)).findFirst().orElseThrow();
  }

  private void requireUnique(Long companyId, String code, Long except) {
    Long count =
        jdbc.queryForObject(
            "select count(*) from rnw_rmu_officer where company_id = :companyId and active"
                + " and ao_code = :code and id <> :except",
            Map.of(
                "companyId",
                companyId,
                "code",
                code,
                "except",
                Objects.requireNonNullElse(except, NONE)),
            Long.class);
    if (count != null && count > 0) {
      throw new BusinessRuleException(
          "RNW_RMU_DUPLICATE", "Duplicate entry detected. The AO code already exists in the list.");
    }
  }

  private static String require(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException("RNW_RMU_REQUIRED", message);
    }
    return value.strip();
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * An RMU Account Officer.
   *
   * @param id id
   * @param aoCode AO code
   * @param aoName name
   * @param active active
   * @param remarks remarks
   * @param createdBy created by
   * @param createdAt created at
   * @param updatedBy updated by
   * @param updatedAt updated at
   */
  public record Officer(
      Long id,
      String aoCode,
      String aoName,
      boolean active,
      String remarks,
      String createdBy,
      Instant createdAt,
      String updatedBy,
      Instant updatedAt) {}

  /**
   * An addition or an update.
   *
   * @param aoCode AO code (addition)
   * @param aoName name
   * @param remarks remarks
   * @param active active (update), null for active
   */
  public record Input(String aoCode, String aoName, String remarks, Boolean active) {}
}
