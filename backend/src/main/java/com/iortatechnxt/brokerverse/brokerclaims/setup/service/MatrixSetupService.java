package com.iortatechnxt.brokerverse.brokerclaims.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusAccess;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusAccessRepository;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The status access matrix on Claims Setup (BRCLM.012/013, FR-CM-041): the Unit Head adds a row
 * (status, role, unit or any unit), another BCL_SETUP user authorizes it before it takes effect,
 * and a row is deactivated, never deleted. The roles offered are those that may record claims or
 * change their status.
 */
@Service
@Transactional
public class MatrixSetupService {

  private static final String ENTITY = "BrokerClaimStatusAccess";

  private static final String CLAIMS_ROLES =
      "select distinct r.code, r.name from sec_role r"
          + " join sec_role_permission p on p.role_id = r.id"
          + " where p.permission in ('BCL_STATUS_UPDATE', 'BCL_RECORD') order by r.code";

  private final StatusAccessRepository access;
  private final LovService lovs;
  private final NamedParameterJdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param access matrix rows
   * @param lovs lists of values
   * @param jdbc named-parameter JDBC (roles)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public MatrixSetupService(
      StatusAccessRepository access,
      LovService lovs,
      NamedParameterJdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.access = access;
    this.lovs = lovs;
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The matrix.
   *
   * @return rows in status, role and unit order
   */
  @Transactional(readOnly = true)
  public List<StatusAccess> rows() {
    return access.findAllByOrderByStatusCodeAscRoleCodeAscUnitCodeAsc();
  }

  /**
   * The roles that may appear in the matrix.
   *
   * @return code and name of each role holding BCL_STATUS_UPDATE or BCL_RECORD
   */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> roles() {
    return jdbc.queryForList(CLAIMS_ROLES, Map.of());
  }

  /**
   * Adds a row (or submits a deactivated one again), waiting for authorization.
   *
   * @param statusCode status
   * @param roleCode role
   * @param unitCode unit, blank for any unit
   * @return the row
   */
  public StatusAccess add(String statusCode, String roleCode, String unitCode) {
    String unit = unitCode == null || unitCode.isBlank() ? null : unitCode;
    validate(statusCode, roleCode, unit);
    StatusAccess row =
        access.findByStatusCodeAndRoleCode(statusCode, roleCode).stream()
            .filter(r -> Objects.equals(r.getUnitCode(), unit))
            .findFirst()
            .orElse(null);
    if (row != null && row.getRecordStatus() != RecordStatus.INACTIVE) {
      throw new BusinessRuleException(
          "BCL_MATRIX_ROW_EXISTS", "The matrix already has this status, role and unit");
    }
    if (row == null) {
      row = access.save(new StatusAccess(statusCode, roleCode, unit));
    } else {
      row.markModified();
    }
    audit.record(ENTITY, row.getId(), AuditAction.SUBMIT, describe(row));
    return row;
  }

  /**
   * Authorizes a row (another user than the maker).
   *
   * @param id row
   * @return the row
   */
  public StatusAccess authorize(Long id) {
    StatusAccess row = get(id);
    row.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.AUTHORIZE, describe(row));
    return row;
  }

  /**
   * Deactivates a row.
   *
   * @param id row
   * @return the row
   */
  public StatusAccess deactivate(Long id) {
    StatusAccess row = get(id);
    row.deactivate();
    audit.record(ENTITY, id, AuditAction.DEACTIVATE, describe(row));
    return row;
  }

  private void validate(String statusCode, String roleCode, String unit) {
    if (statusCode == null || statusCode.isBlank() || roleCode == null || roleCode.isBlank()) {
      throw new BusinessRuleException(
          "BCL_MATRIX_ROW_INCOMPLETE", "Select the status and the role");
    }
    requireValue(ClaimCodes.LOV_STATUS, statusCode);
    if (unit != null) {
      lovs.requireValid(ClaimCodes.LOV_UNIT, unit, ClaimAgeing.today(clock));
    }
    boolean knownRole = roles().stream().anyMatch(r -> roleCode.equals(r.get("code")));
    if (!knownRole) {
      throw new BusinessRuleException(
          "BCL_MATRIX_ROLE_INVALID", roleCode + " is not a claims role");
    }
  }

  private StatusAccess get(Long id) {
    return access.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private void requireValue(String typeCode, String code) {
    boolean exists = lovs.values(typeCode).stream().anyMatch(v -> v.getCode().equals(code));
    if (!exists) {
      throw new ResourceNotFoundException(typeCode, code);
    }
  }

  private static String describe(StatusAccess row) {
    return row.getStatusCode()
        + " / "
        + row.getRoleCode()
        + " / "
        + (row.getUnitCode() == null ? "any unit" : row.getUnitCode());
  }
}
