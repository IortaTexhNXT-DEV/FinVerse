package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

  protected SalesOfficer() {}

  /**
   * Places a user in a team, pending authorization.
   *
   * @param companyId company
   * @param teamCode team
   * @param username user name
   */
  public SalesOfficer(Long companyId, String teamCode, String username) {
    this.companyId = companyId;
    this.teamCode = teamCode;
    this.username = username;
  }

  /**
   * Moves the officer to another team; it must be authorized again.
   *
   * @param newTeamCode team
   */
  public void moveTo(String newTeamCode) {
    this.teamCode = newTeamCode;
    markModified();
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
