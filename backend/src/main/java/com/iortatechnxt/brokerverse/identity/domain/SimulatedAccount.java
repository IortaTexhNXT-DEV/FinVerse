package com.iortatechnxt.brokerverse.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An account of the Enterprise SSO simulator of SIT and UAT: the directory that the simulated
 * sign-in page and the simulated UIDM-ISC events use, so that the flows run end to end before BDOI
 * IT connects the real platform.
 */
@Entity
@Table(name = "idn_sim_directory_account")
public class SimulatedAccount {

  private static final int NAME = 120;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "windows_id", nullable = false, length = 50, updatable = false)
  private String windowsId;

  @Column(name = "user_id", nullable = false, length = 50)
  private String userId;

  @Column(nullable = false, length = NAME)
  private String email;

  @Column(name = "first_name", nullable = false, length = 60)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 60)
  private String lastName;

  @Column(name = "display_name", nullable = false, length = NAME)
  private String displayName;

  @Column(name = "ad_group", length = NAME)
  private String adGroup;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DirectoryStatus status;

  @Column(name = "team_leader_name", length = NAME)
  private String teamLeaderName;

  @Column(name = "team_head_name", length = NAME)
  private String teamHeadName;

  @Column(name = "section_head_name", length = NAME)
  private String sectionHeadName;

  @Column(name = "unit_head_name", length = NAME)
  private String unitHeadName;

  @Column(name = "unit_segment", length = NAME)
  private String unitSegment;

  @Column(length = NAME)
  private String department;

  @Column(length = NAME)
  private String location;

  @Column(name = "uidm_request_no", length = 40)
  private String uidmRequestNo;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected SimulatedAccount() {}

  /**
   * Creates an account.
   *
   * @param account details
   * @param at time
   */
  public SimulatedAccount(DirectoryAccount account, Instant at) {
    this.windowsId = account.windowsId();
    change(account, at);
  }

  /**
   * Changes the details (the Windows ID stays).
   *
   * @param account details
   * @param at time
   */
  public final void change(DirectoryAccount account, Instant at) {
    this.userId = account.userId();
    this.email = account.email();
    this.firstName = account.firstName();
    this.lastName = account.lastName();
    this.displayName = account.fullName();
    this.adGroup = account.adGroup();
    this.status = account.status();
    this.teamLeaderName = account.hierarchy().teamLeader();
    this.teamHeadName = account.hierarchy().teamHead();
    this.sectionHeadName = account.hierarchy().sectionHead();
    this.unitHeadName = account.hierarchy().unitHead();
    this.unitSegment = account.organisation().unitSegment();
    this.department = account.organisation().department();
    this.location = account.organisation().location();
    this.uidmRequestNo = account.uidmRequestNo();
    this.updatedAt = at;
  }

  /**
   * The account as the directory describes it.
   *
   * @return account
   */
  public DirectoryAccount toAccount() {
    return new DirectoryAccount(
        windowsId,
        userId,
        email,
        firstName,
        lastName,
        displayName,
        adGroup,
        status,
        new DirectoryAccount.Hierarchy(teamLeaderName, teamHeadName, sectionHeadName, unitHeadName),
        new DirectoryAccount.Organisation(unitSegment, department, location),
        uidmRequestNo,
        null);
  }

  public Long getId() {
    return id;
  }

  public String getWindowsId() {
    return windowsId;
  }

  public DirectoryStatus getStatus() {
    return status;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
