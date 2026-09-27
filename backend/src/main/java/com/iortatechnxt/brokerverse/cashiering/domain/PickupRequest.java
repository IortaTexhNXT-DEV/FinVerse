package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PickupStatus;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A check tagged "for check pick-up" by Collection (CSHID.009, OQ01/OQ13): queued by pick-up date;
 * printing the ARs of the due requests allocates their AR numbers.
 */
@Entity
@Table(name = "csh_pickup_request")
public class PickupRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "collection_ref", nullable = false, length = 40, updatable = false)
  private String collectionRef;

  @Column(nullable = false, length = 80, updatable = false)
  private String reference;

  @Column(name = "client_code", length = 30, updatable = false)
  private String clientCode;

  @Column(name = "payor_name", nullable = false, length = 250, updatable = false)
  private String payorName;

  @Column(name = "assured_name", length = 250, updatable = false)
  private String assuredName;

  @Column(name = "pickup_date", nullable = false)
  private LocalDate pickupDate;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(nullable = false, length = 100, updatable = false)
  private String requestor;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "check_no", length = 40, updatable = false)
  private String checkNo;

  @Column(name = "check_bank", length = 60, updatable = false)
  private String checkBank;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PickupStatus status = PickupStatus.FOR_PICKUP;

  @Column(name = "receipt_no", length = 40)
  private String receiptNo;

  @Column(name = "payment_id")
  private Long paymentId;

  @Column(name = "printed_at")
  private Instant printedAt;

  protected PickupRequest() {}

  /**
   * Queues a request.
   *
   * @param companyId company
   * @param branchId branch
   * @param details request details
   * @param requestedAt time of request
   */
  public PickupRequest(Long companyId, Long branchId, PickupDetails details, Instant requestedAt) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.collectionRef = details.collectionRef();
    this.reference = details.reference();
    this.clientCode = details.clientCode();
    this.payorName = details.payorName();
    this.assuredName = details.assuredName();
    this.pickupDate = details.pickupDate();
    this.requestor = details.requestor();
    this.amount = details.amount();
    this.currency = details.currency();
    this.checkNo = details.checkNo();
    this.checkBank = details.checkBank();
    this.requestedAt = requestedAt;
  }

  /**
   * Records the AR printed for the request.
   *
   * @param payment payment id
   * @param receipt AR number
   * @param at time
   */
  public void printed(Long payment, String receipt, Instant at) {
    requireQueued();
    this.paymentId = payment;
    this.receiptNo = receipt;
    this.printedAt = at;
    this.status = PickupStatus.AR_PRINTED;
  }

  /** Cancels a queued request. */
  public void cancel() {
    requireQueued();
    this.status = PickupStatus.CANCELLED;
  }

  private void requireQueued() {
    if (status != PickupStatus.FOR_PICKUP) {
      throw new BusinessRuleException(
          "PICKUP_NOT_QUEUED", "Pick-up request " + collectionRef + " is " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getCollectionRef() {
    return collectionRef;
  }

  public String getReference() {
    return reference;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getPayorName() {
    return payorName;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public LocalDate getPickupDate() {
    return pickupDate;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public String getRequestor() {
    return requestor;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getCheckNo() {
    return checkNo;
  }

  public String getCheckBank() {
    return checkBank;
  }

  public PickupStatus getStatus() {
    return status;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public Instant getPrintedAt() {
    return printedAt;
  }

  /**
   * Details of a pick-up request.
   *
   * @param collectionRef Collection reference (unique per company)
   * @param reference invoice, ARN, policy or PN number
   * @param clientCode client, may be null
   * @param payorName payor
   * @param assuredName assured, may be null
   * @param pickupDate pick-up date
   * @param requestor Collection handler
   * @param amount check amount
   * @param currency currency
   * @param checkNo check number, may be null
   * @param checkBank bank, may be null
   */
  public record PickupDetails(
      String collectionRef,
      String reference,
      String clientCode,
      String payorName,
      String assuredName,
      LocalDate pickupDate,
      String requestor,
      BigDecimal amount,
      String currency,
      String checkNo,
      String checkBank) {}
}
