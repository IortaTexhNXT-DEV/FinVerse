package com.iortatechnxt.brokerverse.migration.load.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A renewal advice sent by hand before go-live (object P03; DATA_MIGRATION_DESIGN section 15.1;
 * FR-DM-125): linked to the migrated header by the legacy policy reference, served to Renewal with
 * the header in the go-live extraction so that the advice is not sent again.
 */
@Entity
@Table(name = "mig_ra_sent")
public class RaSent extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "legacy_policy_ref", nullable = false, length = 60, updatable = false)
  private String legacyPolicyRef;

  @Column(name = "account_arn", length = 30)
  private String accountArn;

  @Column(name = "cover_no", nullable = false, length = 40)
  private String coverNo;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Column(name = "ra_sent_date", nullable = false)
  private LocalDate raSentDate;

  @Column(name = "ra_ref", length = 30)
  private String raRef;

  @Column(name = "ra_channel", nullable = false, length = 10)
  private String raChannel;

  @Column(name = "sent_to", length = 250)
  private String sentTo;

  @Column(name = "proposed_insurer", length = 30)
  private String proposedInsurer;

  @Column(length = 3)
  private String currency;

  @Column(name = "proposed_premium", precision = 19, scale = 2)
  private BigDecimal proposedPremium;

  @Column(name = "sent_by", length = 50)
  private String sentBy;

  @Column(name = "tracker_name", nullable = false, length = 120)
  private String trackerName;

  @Column(length = 500)
  private String remarks;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "stage_row_id")
  private Long stageRowId;

  @Column(name = "rolled_back", nullable = false)
  private boolean rolledBack;

  protected RaSent() {}

  /**
   * A loaded advice.
   *
   * @param companyId company
   * @param legacyPolicyRef header reference
   * @param data advice data
   * @param batchId batch
   * @param stageRowId staged row
   */
  public RaSent(Long companyId, String legacyPolicyRef, Data data, Long batchId, Long stageRowId) {
    this.companyId = companyId;
    this.legacyPolicyRef = legacyPolicyRef;
    assign(data);
    this.batchId = batchId;
    this.stageRowId = stageRowId;
  }

  /**
   * Replaces the advice data (rerun of a corrected row).
   *
   * @param data advice data
   */
  public void apply(Data data) {
    assign(data);
  }

  private void assign(Data data) {
    this.accountArn = data.accountArn();
    this.coverNo = data.coverNo();
    this.expiryDate = data.expiryDate();
    this.raSentDate = data.raSentDate();
    this.raRef = data.raRef();
    this.raChannel = data.raChannel();
    this.sentTo = data.sentTo();
    this.proposedInsurer = data.proposedInsurer();
    this.currency = data.currency();
    this.proposedPremium = data.proposedPremium();
    this.sentBy = data.sentBy();
    this.trackerName = data.trackerName();
    this.remarks = data.remarks();
  }

  /** Undone by a rollback of its batch. */
  public void rollBack() {
    this.rolledBack = true;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getLegacyPolicyRef() {
    return legacyPolicyRef;
  }

  public String getAccountArn() {
    return accountArn;
  }

  public String getCoverNo() {
    return coverNo;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public LocalDate getRaSentDate() {
    return raSentDate;
  }

  public String getRaRef() {
    return raRef;
  }

  public String getRaChannel() {
    return raChannel;
  }

  public String getSentTo() {
    return sentTo;
  }

  public String getProposedInsurer() {
    return proposedInsurer;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getProposedPremium() {
    return proposedPremium;
  }

  public String getSentBy() {
    return sentBy;
  }

  public String getTrackerName() {
    return trackerName;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getStageRowId() {
    return stageRowId;
  }

  public boolean isRolledBack() {
    return rolledBack;
  }

  /**
   * Data of an advice.
   *
   * @param accountArn ARN of the migrated header
   * @param coverNo cover number
   * @param expiryDate expiry of the term
   * @param raSentDate date sent
   * @param raRef reference of the tracker
   * @param raChannel EMAIL, COURIER, HAND or OTHER
   * @param sentTo recipient
   * @param proposedInsurer proposed insurer (BIBS code)
   * @param currency currency of the premium quoted
   * @param proposedPremium premium quoted
   * @param sentBy sender (BIBS user)
   * @param trackerName tracker and sheet
   * @param remarks remarks
   */
  public record Data(
      String accountArn,
      String coverNo,
      LocalDate expiryDate,
      LocalDate raSentDate,
      String raRef,
      String raChannel,
      String sentTo,
      String proposedInsurer,
      String currency,
      BigDecimal proposedPremium,
      String sentBy,
      String trackerName,
      String remarks) {}
}
