package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A row of the status access matrix (BRCLM.012/013; CLAIMS_BROKING_DESIGN 5.3): the status may be
 * selected by a holder of the role in the unit ({@code null} = any unit). Maker-checker: a row is
 * used only once another user authorized it; a row is deactivated, never deleted.
 */
@Entity
@Table(name = "bcl_status_access")
public class StatusAccess extends AuthorizableEntity {

  @Column(name = "status_code", nullable = false, length = 40, updatable = false)
  private String statusCode;

  @Column(name = "role_code", nullable = false, length = 40, updatable = false)
  private String roleCode;

  @Column(name = "unit_code", length = 40, updatable = false)
  private String unitCode;

  protected StatusAccess() {}

  /**
   * Creates a row waiting for authorization.
   *
   * @param statusCode status ({@code BCL_CLAIM_STATUS})
   * @param roleCode role
   * @param unitCode unit ({@code BCL_UNIT}), null for any unit
   */
  public StatusAccess(String statusCode, String roleCode, String unitCode) {
    this.statusCode = statusCode;
    this.roleCode = roleCode;
    this.unitCode = unitCode;
  }

  /**
   * Whether the row lets a user of its role in a unit select the status.
   *
   * @param userUnit unit of the user
   * @return true when active and the unit matches (or the row is for any unit)
   */
  public boolean admits(String userUnit) {
    return isActive() && (unitCode == null || unitCode.equals(userUnit));
  }

  public String getStatusCode() {
    return statusCode;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public String getUnitCode() {
    return unitCode;
  }
}
