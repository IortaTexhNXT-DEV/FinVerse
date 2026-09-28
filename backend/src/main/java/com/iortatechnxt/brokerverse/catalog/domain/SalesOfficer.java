package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** An account officer (user) of a sales team (BRNB.075 production per user). */
@Entity
@Table(name = "cat_sales_officer")
public class SalesOfficer extends AuthorizableEntity implements CatalogRecord {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "team_code", nullable = false, length = 20)
  private String teamCode;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(name = "assigned_since")
  private LocalDate assignedSince;

  @Column(name = "status_reason", length = 500)
  private String statusReason;

  protected SalesOfficer() {}

  /**
   * Places a user in a team, pending authorization.
   *
   * @param companyId company
   * @param teamCode team
   * @param username user name
   * @param since date of the assignment
   */
  public SalesOfficer(Long companyId, String teamCode, String username, LocalDate since) {
    this.companyId = companyId;
    this.teamCode = teamCode;
    this.username = username;
    this.assignedSince = since;
  }

  /**
   * Moves the officer to another team (or places a removed officer again); it must be authorized
   * again.
   *
   * @param newTeamCode team
   * @param since date of the new assignment
   */
  public void moveTo(String newTeamCode, LocalDate since) {
    this.teamCode = newTeamCode;
    this.assignedSince = since;
    this.statusReason = null;
    markModified();
  }

  /**
   * Removes the officer from the team with the reason given by the maintainer.
   *
   * @param reason reason
   */
  public void remove(String reason) {
    deactivate();
    this.statusReason = reason;
  }

  /**
   * Date of the current team assignment (the creation date for officers placed before it was kept).
   *
   * @return date, null when unknown
   */
  public LocalDate getAssignedSince() {
    return assignedSince;
  }

  /**
   * Reason of the removal from the team.
   *
   * @return reason, null when none
   */
  public String getStatusReason() {
    return statusReason;
  }

  @Override
  public String catalogReference() {
    return username;
  }

  @Override
  public String catalogDescription() {
    return username + " in team " + teamCode;
  }

  @Override
  public Long catalogCompanyId() {
    return companyId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getTeamCode() {
    return teamCode;
  }

  public String getUsername() {
    return username;
  }
}
