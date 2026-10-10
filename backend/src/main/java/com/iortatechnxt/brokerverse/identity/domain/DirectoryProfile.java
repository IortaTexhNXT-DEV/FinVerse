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
 * The directory details of a user retrieved from the Enterprise SSO platform, with the last
 * synchronisation (BDOI FRS FRUM.002.01, FRUM.002.02 and FRUM.003.03: the latest synchronisation
 * status and time of the user).
 */
@Entity
@Table(name = "idn_directory_profile")
public class DirectoryProfile {

  private static final int NAME = 120;
  private static final int MESSAGE = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(name = "windows_id", nullable = false, length = 50)
  private String windowsId;

  @Column(name = "ad_email", length = NAME)
  private String adEmail;

  @Column(name = "ad_group", length = NAME)
  private String adGroup;

  @Enumerated(EnumType.STRING)
  @Column(name = "ad_status", length = 20)
  private DirectoryStatus adStatus;

  @Column(name = "ad_sync_at")
  private Instant adSyncAt;

  @Column(name = "first_name", length = 60)
  private String firstName;

  @Column(name = "last_name", length = 60)
  private String lastName;

  @Column(name = "display_name", length = NAME)
  private String displayName;

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

  @Enumerated(EnumType.STRING)
  @Column(name = "last_event_type", length = 20)
  private IdentityEventType lastEventType;

  @Column(name = "last_event_at")
  private Instant lastEventAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_event_source", length = 20)
  private IdentityEventSource lastEventSource;

  @Column(name = "sync_status", nullable = false, length = 20)
  private String syncStatus = SYNCED;

  @Column(name = "sync_message", length = MESSAGE)
  private String syncMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, updatable = false, length = 50)
  private String createdBy;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "updated_by", length = 50)
  private String updatedBy;

  /** Status of a synchronisation that worked. */
  public static final String SYNCED = "SYNCED";

  /** Status of a synchronisation that failed. */
  public static final String FAILED = "FAILED";

  protected DirectoryProfile() {}

  /**
   * Creates the profile of a user.
   *
   * @param username user
   * @param windowsId Windows ID
   * @param at time
   * @param by actor
   */
  public DirectoryProfile(String username, String windowsId, Instant at, String by) {
    this.username = username;
    this.windowsId = windowsId;
    this.createdAt = at;
    this.createdBy = by;
  }

  /**
   * Takes the details of the account and the event that brought them.
   *
   * @param account account
   * @param event the event
   * @param at time of the synchronisation
   * @param by actor
   */
  public void synchronise(DirectoryAccount account, IdentityEvent event, Instant at, String by) {
    this.windowsId = account.windowsId();
    this.adEmail = account.email();
    this.adGroup = account.adGroup();
    this.adStatus = account.status();
    this.adSyncAt = at;
    this.firstName = account.firstName();
    this.lastName = account.lastName();
    this.displayName = account.displayName();
    this.teamLeaderName = account.hierarchy().teamLeader();
    this.teamHeadName = account.hierarchy().teamHead();
    this.sectionHeadName = account.hierarchy().sectionHead();
    this.unitHeadName = account.hierarchy().unitHead();
    this.unitSegment = account.organisation().unitSegment();
    this.department = account.organisation().department();
    this.location = account.organisation().location();
    if (account.uidmRequestNo() != null && !account.uidmRequestNo().isBlank()) {
      this.uidmRequestNo = account.uidmRequestNo();
    }
    this.lastEventType = event.getEventType();
    this.lastEventAt = event.getReceivedAt();
    this.lastEventSource = event.getSource();
    this.syncStatus = SYNCED;
    this.syncMessage = null;
    this.updatedAt = at;
    this.updatedBy = by;
  }

  /**
   * Records a synchronisation that failed (shown on the user for review).
   *
   * @param message why
   * @param at time
   * @param by actor
   */
  public void failed(String message, Instant at, String by) {
    this.syncStatus = FAILED;
    this.syncMessage =
        message == null || message.length() <= MESSAGE ? message : message.substring(0, MESSAGE);
    this.updatedAt = at;
    this.updatedBy = by;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getWindowsId() {
    return windowsId;
  }

  public String getAdEmail() {
    return adEmail;
  }

  public String getAdGroup() {
    return adGroup;
  }

  public DirectoryStatus getAdStatus() {
    return adStatus;
  }

  public Instant getAdSyncAt() {
    return adSyncAt;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getTeamLeaderName() {
    return teamLeaderName;
  }

  public String getTeamHeadName() {
    return teamHeadName;
  }

  public String getSectionHeadName() {
    return sectionHeadName;
  }

  public String getUnitHeadName() {
    return unitHeadName;
  }

  public String getUnitSegment() {
    return unitSegment;
  }

  public String getDepartment() {
    return department;
  }

  public String getLocation() {
    return location;
  }

  public String getUidmRequestNo() {
    return uidmRequestNo;
  }

  public IdentityEventType getLastEventType() {
    return lastEventType;
  }

  public Instant getLastEventAt() {
    return lastEventAt;
  }

  public IdentityEventSource getLastEventSource() {
    return lastEventSource;
  }

  public String getSyncStatus() {
    return syncStatus;
  }

  public String getSyncMessage() {
    return syncMessage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }
}
