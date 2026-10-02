package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A claims handler of the register (BRCLM.012, NFR p.37; CLAIMS_BROKING_DESIGN 5.3): the unit of
 * the user drives the status access matrix and the default assignment, the team is shown on the
 * reports. Maintained by the Unit Head on Claims Setup.
 */
@Entity
@Table(name = "bcl_handler")
public class ClaimHandler extends BaseEntity {

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(name = "unit_code", nullable = false, length = 40)
  private String unitCode;

  @Column(length = 60)
  private String team;

  @Column(nullable = false)
  private boolean active;

  protected ClaimHandler() {}

  /**
   * Registers a handler.
   *
   * @param username user
   * @param unitCode unit ({@code BCL_UNIT})
   * @param team team, may be null
   */
  public ClaimHandler(String username, String unitCode, String team) {
    this.username = username;
    this.unitCode = unitCode;
    this.team = team;
    this.active = true;
  }

  /**
   * Changes the unit, team and active flag.
   *
   * @param newUnit unit
   * @param newTeam team
   * @param isActive whether the handler is active
   */
  public void update(String newUnit, String newTeam, boolean isActive) {
    this.unitCode = newUnit;
    this.team = newTeam;
    this.active = isActive;
  }

  public String getUsername() {
    return username;
  }

  public String getUnitCode() {
    return unitCode;
  }

  public String getTeam() {
    return team;
  }

  public boolean isActive() {
    return active;
  }
}
