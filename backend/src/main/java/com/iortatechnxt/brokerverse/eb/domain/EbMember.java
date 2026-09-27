package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A member of a roster version (FR-EB-054): employee number, name, birth date, gender, civil
 * status, plan, dependants and effective dates. Minimal fields and no health data until the master
 * list fields are confirmed (EBQ15).
 */
@Entity
@Table(name = "eb_member")
public class EbMember extends BaseEntity {

  /** Status of a member. */
  public enum Status {
    /** Covered. */
    ACTIVE,
    /** Deleted by a member change. */
    DELETED
  }

  @Column(name = "roster_version_id", nullable = false, updatable = false)
  private Long rosterVersionId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "policy_year", nullable = false, updatable = false)
  private int policyYear;

  @Column(name = "employee_no", nullable = false, length = 30, updatable = false)
  private String employeeNo;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  @Column(length = 10)
  private String gender;

  @Column(name = "civil_status", length = 20)
  private String civilStatus;

  @Column(name = "plan_code", nullable = false, length = 30)
  private String planCode;

  @Column(nullable = false)
  private int dependants;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.ACTIVE;

  protected EbMember() {}

  /**
   * Adds a member to a roster version.
   *
   * @param version roster version
   * @param employeeNo employee number
   * @param data member data
   * @param effectiveFrom effective date
   */
  public EbMember(EbRosterVersion version, String employeeNo, Data data, LocalDate effectiveFrom) {
    this.rosterVersionId = version.getId();
    this.programmeId = version.getProgrammeId();
    this.policyYear = version.getPolicyYear();
    this.employeeNo = employeeNo;
    this.effectiveFrom = effectiveFrom;
    apply(data);
  }

  /**
   * Replaces the member data (change of data).
   *
   * @param data member data
   */
  public void update(Data data) {
    apply(data);
  }

  /**
   * Moves the member to another plan.
   *
   * @param plan plan code
   */
  public void changePlan(String plan) {
    this.planCode = plan;
  }

  /**
   * Deletes the member from a date.
   *
   * @param lastDay last day covered
   */
  public void delete(LocalDate lastDay) {
    this.status = Status.DELETED;
    this.effectiveTo = lastDay;
  }

  private void apply(Data data) {
    this.lastName = data.lastName();
    this.firstName = data.firstName();
    this.birthDate = data.birthDate();
    this.gender = data.gender();
    this.civilStatus = data.civilStatus();
    this.planCode = data.planCode();
    this.dependants = data.dependants() == null ? 0 : data.dependants();
  }

  /**
   * Display name "Last, First".
   *
   * @return name
   */
  public String displayName() {
    return lastName + ", " + firstName;
  }

  public Long getRosterVersionId() {
    return rosterVersionId;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public String getLastName() {
    return lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getGender() {
    return gender;
  }

  public String getCivilStatus() {
    return civilStatus;
  }

  public String getPlanCode() {
    return planCode;
  }

  public int getDependants() {
    return dependants;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public Status getStatus() {
    return status;
  }

  /**
   * Member data.
   *
   * @param lastName last name
   * @param firstName first name
   * @param birthDate birth date
   * @param gender MALE or FEMALE, may be null
   * @param civilStatus list CIVIL_STATUS, may be null
   * @param planCode plan
   * @param dependants number of dependants, may be null (none)
   */
  public record Data(
      String lastName,
      String firstName,
      LocalDate birthDate,
      String gender,
      String civilStatus,
      String planCode,
      Integer dependants) {}
}
