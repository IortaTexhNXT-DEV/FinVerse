package com.iortatechnxt.brokerverse.organization.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Branch / office / customer centre of a company (Office Master). */
@Entity
@Table(name = "org_branch")
public class Branch extends AuthorizableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(nullable = false, length = 10)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 60)
  private String region;

  @Column(length = 300)
  private String address;

  @Column(name = "opening_date", nullable = false)
  private LocalDate openingDate;

  @Column(name = "head_office", nullable = false)
  private boolean headOffice;

  @Column(name = "forex_authorized", nullable = false)
  private boolean forexAuthorized;

  @Column(name = "contact_phone", length = 40)
  private String contactPhone;

  @Column(name = "contact_email", length = 120)
  private String contactEmail;

  @Column(name = "manager_name", length = 120)
  private String managerName;

  /** Comma separated ISO day-of-week numbers that are weekly holidays, e.g. "6,7". */
  @Column(name = "weekly_holidays", length = 20)
  private String weeklyHolidays;

  protected Branch() {}

  /**
   * Creates a branch.
   *
   * @param company owning company
   * @param code branch code, unique within the company
   * @param name branch name
   * @param openingDate date of opening
   */
  public Branch(Company company, String code, String name, LocalDate openingDate) {
    this.company = company;
    this.code = code;
    this.name = name;
    this.openingDate = openingDate;
  }

  public Company getCompany() {
    return company;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public LocalDate getOpeningDate() {
    return openingDate;
  }

  public boolean isHeadOffice() {
    return headOffice;
  }

  public void setHeadOffice(boolean headOffice) {
    this.headOffice = headOffice;
  }

  public boolean isForexAuthorized() {
    return forexAuthorized;
  }

  public void setForexAuthorized(boolean forexAuthorized) {
    this.forexAuthorized = forexAuthorized;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getManagerName() {
    return managerName;
  }

  public void setManagerName(String managerName) {
    this.managerName = managerName;
  }

  public String getWeeklyHolidays() {
    return weeklyHolidays;
  }

  public void setWeeklyHolidays(String weeklyHolidays) {
    this.weeklyHolidays = weeklyHolidays;
  }
}
