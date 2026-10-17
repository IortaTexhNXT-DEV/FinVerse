package com.iortatechnxt.brokerverse.collections.feed.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.OutboxStatus;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An item Collections hands to Operations through an inbound {@code COLLECTION_*} feed
 * (COLLECTIONS_DESIGN 2.2): a BIR 2307 tag for Cashiering, a direct payment account for Commission
 * or a check pick-up request for Cashiering. The consumer pulls it with {@code
 * CollectionFeed.pending} and acknowledges it once taken.
 */
@Entity
@Table(name = "clx_outbox")
public class OutboxItem extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "feed_code", nullable = false, length = 40, updatable = false)
  private String feedCode;

  @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false)
  private String idempotencyKey;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 4000, updatable = false)
  private String fields;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private OutboxStatus status = OutboxStatus.PENDING;

  @Column(name = "taken_at")
  private Instant takenAt;

  @Column(name = "source_disposition_id", updatable = false)
  private Long sourceDispositionId;

  protected OutboxItem() {}

  /**
   * Queues an item.
   *
   * @param companyId company
   * @param feed feed and idempotency key
   * @param invoiceNo invoice
   * @param fields fields as JSON
   * @param sourceDispositionId disposition that created it
   */
  public OutboxItem(
      Long companyId, FeedKey feed, String invoiceNo, String fields, Long sourceDispositionId) {
    this.companyId = companyId;
    this.feedCode = feed.feedCode();
    this.idempotencyKey = feed.key();
    this.invoiceNo = invoiceNo;
    this.fields = fields;
    this.sourceDispositionId = sourceDispositionId;
  }

  /**
   * The consumer took the item.
   *
   * @param at time
   * @return true when it was pending
   */
  public boolean taken(Instant at) {
    if (status != OutboxStatus.PENDING) {
      return false;
    }
    status = OutboxStatus.TAKEN;
    takenAt = at;
    return true;
  }

  /**
   * Withdraws a pending item (its disposition was superseded).
   *
   * @return true when it was pending
   */
  public boolean cancel() {
    if (status != OutboxStatus.PENDING) {
      return false;
    }
    status = OutboxStatus.CANCELLED;
    return true;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getFeedCode() {
    return feedCode;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getFields() {
    return fields;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public Instant getTakenAt() {
    return takenAt;
  }

  public Long getSourceDispositionId() {
    return sourceDispositionId;
  }

  /**
   * Feed and idempotency key of an item.
   *
   * @param feedCode feed
   * @param key idempotency key
   */
  public record FeedKey(String feedCode, String key) {}
}
