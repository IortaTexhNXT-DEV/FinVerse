package com.iortatechnxt.brokerverse.collections.feed.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An item Operations sent to Collections through an outbound {@code COLLECTION_*} feed
 * (COLLECTIONS_DESIGN 2.2): a direct payment account returned by the insurer (CMRID.009) or a
 * refund (BRCLXN.040), with the flow-in run it arrived in and what Collections did with it.
 */
@Entity
@Table(name = "clx_inbox")
public class InboxItem extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "feed_code", nullable = false, length = 40, updatable = false)
  private String feedCode;

  @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false)
  private String idempotencyKey;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 4000, updatable = false)
  private String fields;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "run_no", length = 40, updatable = false)
  private String runNo;

  @Column(name = "item_id")
  private Long itemId;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(length = 500)
  private String message;

  protected InboxItem() {}

  /**
   * Receives an item.
   *
   * @param companyId company
   * @param feed feed and key
   * @param invoiceNo invoice named by the item, may be null
   * @param fields fields as JSON
   * @param arrival run number and time
   */
  public InboxItem(
      Long companyId, OutboxItem.FeedKey feed, String invoiceNo, String fields, Arrival arrival) {
    this.companyId = companyId;
    this.feedCode = feed.feedCode();
    this.idempotencyKey = feed.key();
    this.invoiceNo = invoiceNo;
    this.fields = fields;
    this.runNo = arrival.runNo();
    this.receivedAt = arrival.at();
  }

  /**
   * Records what was done with the item.
   *
   * @param item collection item it concerned, may be null
   * @param outcome message
   * @param at time
   */
  public void processed(Long item, String outcome, Instant at) {
    this.itemId = item;
    this.message = outcome;
    this.processedAt = at;
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

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public String getRunNo() {
    return runNo;
  }

  public Long getItemId() {
    return itemId;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public String getMessage() {
    return message;
  }

  /**
   * How an item arrived.
   *
   * @param runNo flow-in run
   * @param at received at
   */
  public record Arrival(String runNo, Instant at) {}
}
