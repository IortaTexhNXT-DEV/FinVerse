package com.iortatechnxt.brokerverse.placement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A CLPC billing batch (BRNB.067): the CBG Fire accounts awaiting payment billed on one file. The
 * file is regenerated from the items on every download (.xlsx or .ods); the transport to CLPC is
 * parked (Q28).
 */
@Entity
@Table(name = "plc_billing_batch")
public class BillingBatch extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 30, updatable = false)
  private String batchNo;

  @Column(name = "billing_date", nullable = false, updatable = false)
  private LocalDate billingDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BillingBatchStatus status = BillingBatchStatus.GENERATED;

  @Column(name = "item_count", nullable = false)
  private int itemCount;

  @Column(name = "total_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalPremium = BigDecimal.ZERO;

  @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<BillingItem> items = new ArrayList<>();

  protected BillingBatch() {}

  /**
   * Creates an empty batch.
   *
   * @param companyId company
   * @param batchNo batch number
   * @param billingDate billing date
   */
  public BillingBatch(Long companyId, String batchNo, LocalDate billingDate) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.billingDate = billingDate;
  }

  /**
   * Adds an account to the batch.
   *
   * @param line billing line
   * @return the item
   */
  public BillingItem add(BillingLine line) {
    BillingItem item = new BillingItem(this, items.size() + 1, line);
    items.add(item);
    itemCount = items.size();
    totalPremium = totalPremium.add(line.premium());
    return item;
  }

  /** Marks that a payment report was received for the batch. */
  public void reportReceived() {
    if (status == BillingBatchStatus.GENERATED) {
      status = BillingBatchStatus.REPORT_RECEIVED;
    }
  }

  /** Closes the batch once its payment report is confirmed. */
  public void close() {
    status = BillingBatchStatus.CLOSED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public LocalDate getBillingDate() {
    return billingDate;
  }

  public BillingBatchStatus getStatus() {
    return status;
  }

  public int getItemCount() {
    return itemCount;
  }

  public BigDecimal getTotalPremium() {
    return totalPremium;
  }

  public List<BillingItem> getItems() {
    return List.copyOf(items);
  }
}
