package com.iortatechnxt.brokerverse.csf.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A contact change made from the Customer Servicing Facility (FR-CSF-021, 022, 040): applied to the
 * client master, refused (a field the contact centre cannot change) or referred to the fulfilment
 * unit, with the verification of the caller, the reason, the field rows with the values before and
 * after, and the legacy write-back state. Number {@code CSF-yyyy-nnnnnn}.
 */
@Entity
@Table(name = "csf_contact_change")
public class CsfContactChange extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "change_no", nullable = false, length = 30, updatable = false)
  private String changeNo;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "client_name", nullable = false, length = 250, updatable = false)
  private String clientName;

  @Column(name = "verification_id", updatable = false)
  private Long verificationId;

  @Column(length = 20, updatable = false)
  private String channel;

  @Column(name = "reason_code", length = 40, updatable = false)
  private String reasonCode;

  @Column(length = 500, updatable = false)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private ChangeStatus status;

  @Column(name = "applied_at", nullable = false, updatable = false)
  private Instant appliedAt;

  @Column(nullable = false, length = 50, updatable = false)
  private String agent;

  @Enumerated(EnumType.STRING)
  @Column(name = "sync_status", nullable = false, length = 20)
  private SyncStatus syncStatus;

  @Column(name = "handoff_id")
  private Long handoffId;

  @ElementCollection
  @CollectionTable(name = "csf_contact_change_field", joinColumns = @JoinColumn(name = "change_id"))
  @OrderColumn(name = "field_index")
  private final List<ChangedField> fields = new ArrayList<>();

  protected CsfContactChange() {}

  /**
   * Records a change.
   *
   * @param header number, client, status, time and agent
   * @param request verification, channel, reason and remarks
   * @param fields field rows
   */
  public CsfContactChange(Header header, Request request, List<ChangedField> fields) {
    this.companyId = header.client().companyId();
    this.clientId = header.client().clientId();
    this.clientCode = header.client().clientCode();
    this.clientName = header.clientName();
    this.changeNo = header.changeNo();
    this.status = header.status();
    this.appliedAt = header.at();
    this.agent = header.agent();
    this.verificationId = request.verificationId();
    this.channel = request.channel();
    this.reasonCode = request.reasonCode();
    this.remarks = request.remarks();
    this.syncStatus = SyncStatus.NOT_REQUIRED;
    this.fields.addAll(fields);
  }

  /**
   * Sets the legacy write-back state.
   *
   * @param status state
   */
  public void syncState(SyncStatus status) {
    this.syncStatus = status;
  }

  /**
   * Keeps the Operations hand-off of a referral.
   *
   * @param id hand-off id
   */
  public void handedOff(Long id) {
    this.handoffId = id;
  }

  /**
   * Number, client and state of a change.
   *
   * @param changeNo change number
   * @param client client
   * @param clientName client name
   * @param status state
   * @param at time
   * @param agent agent
   */
  public record Header(
      String changeNo,
      CsfVerification.ClientRef client,
      String clientName,
      ChangeStatus status,
      Instant at,
      String agent) {}

  /**
   * What the caller asked for.
   *
   * @param verificationId verification of the caller, may be null for a referral
   * @param channel channel (list CSF_CHANNEL)
   * @param reasonCode reason (list CSF_CHANGE_REASON), may be null
   * @param remarks remarks, may be null
   */
  public record Request(Long verificationId, String channel, String reasonCode, String remarks) {}

  public Long getCompanyId() {
    return companyId;
  }

  public String getChangeNo() {
    return changeNo;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getClientName() {
    return clientName;
  }

  public Long getVerificationId() {
    return verificationId;
  }

  public String getChannel() {
    return channel;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public ChangeStatus getStatus() {
    return status;
  }

  public Instant getAppliedAt() {
    return appliedAt;
  }

  public String getAgent() {
    return agent;
  }

  public SyncStatus getSyncStatus() {
    return syncStatus;
  }

  public Long getHandoffId() {
    return handoffId;
  }

  public List<ChangedField> getFields() {
    return List.copyOf(fields);
  }
}
