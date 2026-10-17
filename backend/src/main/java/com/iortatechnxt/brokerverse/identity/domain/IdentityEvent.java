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
 * A provisioning event received from UIDM-ISC or the Enterprise SSO platform, or an on-demand
 * synchronisation, with its outcome (BDOI FRS FRUM.002.02: "log all user synchronization activities
 * and processing results"; "identify and record synchronization failures for administrative
 * review"). A refused or failed event can be reprocessed.
 */
@Entity
@Table(name = "idn_identity_event")
public class IdentityEvent {

  private static final int MESSAGE = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private IdentityEventSource source;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 20, updatable = false)
  private IdentityEventType eventType;

  @Column(name = "windows_id", length = 50, updatable = false)
  private String windowsId;

  @Column(name = "user_id", length = 50, updatable = false)
  private String userId;

  @Column(name = "uidm_request_no", length = 40, updatable = false)
  private String uidmRequestNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "ad_status", length = 20, updatable = false)
  private DirectoryStatus adStatus;

  @Column(nullable = false, updatable = false, columnDefinition = "text")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IdentityEventStatus status;

  @Column(length = 50)
  private String username;

  @Column(length = MESSAGE)
  private String message;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "processed_by", length = 50)
  private String processedBy;

  @Column(nullable = false)
  private int attempts = 1;

  protected IdentityEvent() {}

  /**
   * Records a received event.
   *
   * @param receivedAt time received
   * @param source source
   * @param eventType what it asks for
   * @param account the account it carries
   * @param payload the event as received (JSON)
   */
  public IdentityEvent(
      Instant receivedAt,
      IdentityEventSource source,
      IdentityEventType eventType,
      DirectoryAccount account,
      String payload) {
    this.receivedAt = receivedAt;
    this.source = source;
    this.eventType = eventType;
    this.windowsId = account.windowsId();
    this.userId = account.userId();
    this.uidmRequestNo = account.uidmRequestNo();
    this.adStatus = account.status();
    this.payload = payload;
    this.status = IdentityEventStatus.FAILED;
  }

  /**
   * Records the outcome of a processing.
   *
   * @param outcome outcome
   * @param user the user concerned, may be null
   * @param text what was done or why it was refused
   * @param at time
   * @param by who processed it (SYSTEM, or the user who reprocessed it)
   */
  public void processed(
      IdentityEventStatus outcome, String user, String text, Instant at, String by) {
    this.status = outcome;
    this.username = user;
    this.message = text == null || text.length() <= MESSAGE ? text : text.substring(0, MESSAGE);
    this.processedAt = at;
    this.processedBy = by;
  }

  /** Counts a further processing (reprocess). */
  public void retried() {
    this.attempts++;
  }

  /**
   * Whether the event may be processed again.
   *
   * @return true when refused or failed
   */
  public boolean reprocessable() {
    return status == IdentityEventStatus.REFUSED || status == IdentityEventStatus.FAILED;
  }

  public Long getId() {
    return id;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public IdentityEventSource getSource() {
    return source;
  }

  public IdentityEventType getEventType() {
    return eventType;
  }

  public String getWindowsId() {
    return windowsId;
  }

  public String getUserId() {
    return userId;
  }

  public String getUidmRequestNo() {
    return uidmRequestNo;
  }

  public DirectoryStatus getAdStatus() {
    return adStatus;
  }

  public String getPayload() {
    return payload;
  }

  public IdentityEventStatus getStatus() {
    return status;
  }

  public String getUsername() {
    return username;
  }

  public String getMessage() {
    return message;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public String getProcessedBy() {
    return processedBy;
  }

  public int getAttempts() {
    return attempts;
  }
}
