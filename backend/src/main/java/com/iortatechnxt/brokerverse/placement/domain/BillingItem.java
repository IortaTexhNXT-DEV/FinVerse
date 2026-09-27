package com.iortatechnxt.brokerverse.placement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** One account on a CLPC billing batch (BRNB.067) with its payment outcome. */
@Entity
@Table(name = "plc_billing_item")
public class BillingItem {

  private static final String PN_SEPARATOR = ", ";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_id", nullable = false, updatable = false)
  private BillingBatch batch;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "pn_numbers", length = 300)
  private String pnNumbers;

  @Column(name = "loan_application_no", length = 40)
  private String loanApplicationNo;

  @Column(name = "booking_date")
  private LocalDate bookingDate;

  @Column(nullable = false, length = 250)
  private String borrower;

  @Column(name = "originating_unit", length = 40)
  private String originatingUnit;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal premium;

  @Column(name = "bdoi_location", length = 40)
  private String bdoiLocation;

  @Column(nullable = false)
  private boolean amortised;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false, length = 20)
  private BillingItemStatus paymentStatus = BillingItemStatus.BILLED;

  protected BillingItem() {}

  BillingItem(BillingBatch batch, int lineNo, BillingLine line) {
    this.batch = batch;
    this.lineNo = lineNo;
    this.accountId = line.accountId();
    this.arn = line.arn();
    this.pnNumbers =
        line.pnNumbers().isEmpty() ? null : String.join(PN_SEPARATOR, line.pnNumbers());
    this.loanApplicationNo = line.loanApplicationNo();
    this.bookingDate = line.bookingDate();
    this.borrower = line.borrower();
    this.originatingUnit = line.originatingUnit();
    this.premium = line.premium();
    this.bdoiLocation = line.bdoiLocation();
    this.amortised = line.amortised();
  }

  /**
   * Records the outcome of the payment report.
   *
   * @param paid true when paid
   */
  public void markPayment(boolean paid) {
    this.paymentStatus = paid ? BillingItemStatus.PAID : BillingItemStatus.UNPAID;
  }

  /**
   * Whether a CLPC reference (PN or loan application number) identifies this item.
   *
   * @param reference reference, upper case and trimmed
   * @return true on a match
   */
  public boolean isIdentifiedBy(String reference) {
    return sameReference(reference, loanApplicationNo)
        || getPnNumberList().stream().anyMatch(pn -> sameReference(reference, pn));
  }

  private static boolean sameReference(String reference, String value) {
    return value != null && String.CASE_INSENSITIVE_ORDER.compare(reference, value) == 0;
  }

  public Long getId() {
    return id;
  }

  public BillingBatch getBatch() {
    return batch;
  }

  public int getLineNo() {
    return lineNo;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getArn() {
    return arn;
  }

  public String getPnNumbers() {
    return pnNumbers;
  }

  /**
   * The promissory note numbers one by one.
   *
   * @return PN numbers
   */
  public List<String> getPnNumberList() {
    return pnNumbers == null ? List.of() : Arrays.asList(pnNumbers.split(PN_SEPARATOR));
  }

  public String getLoanApplicationNo() {
    return loanApplicationNo;
  }

  public LocalDate getBookingDate() {
    return bookingDate;
  }

  public String getBorrower() {
    return borrower;
  }

  public String getOriginatingUnit() {
    return originatingUnit;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public String getBdoiLocation() {
    return bdoiLocation;
  }

  public boolean isAmortised() {
    return amortised;
  }

  public BillingItemStatus getPaymentStatus() {
    return paymentStatus;
  }
}
