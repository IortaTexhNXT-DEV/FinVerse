package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * An acknowledgement receipt (premium or non-premium payment, CSHID.001) or a Head Office official
 * receipt (BDOI income, CSHID.002). One class holds both; the kind drives the series, the
 * accounting event and the printed document (OPERATIONS_DESIGN principle 1).
 */
@Entity(name = "CashReceipt")
@Table(name = "csh_receipt")
public class Receipt extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "receipt_no", nullable = false, length = 40, updatable = false)
  private String receiptNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 2, updatable = false)
  private ReceiptKind kind;

  @Column(name = "receipt_class", nullable = false, length = 30, updatable = false)
  private String receiptClass;

  @Column(name = "series_id", updatable = false)
  private Long seriesId;

  @Column(name = "receipt_date", nullable = false, updatable = false)
  private LocalDate receiptDate;

  @Column(name = "payor_code", length = 30)
  private String payorCode;

  @Column(name = "payor_name", nullable = false, length = 250)
  private String payorName;

  @Column(name = "assured_name", length = 250)
  private String assuredName;

  @Column(name = "sales_unit", length = 40)
  private String salesUnit;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "book_rate", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal bookRate;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal gross;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal vat;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal wtax;

  @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal appliedAmount = BigDecimal.ZERO.setScale(2);

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_mode", nullable = false, length = 20, updatable = false)
  private PaymentMode mode;

  @Column(name = "check_no", length = 40, updatable = false)
  private String checkNo;

  @Column(name = "check_bank", length = 60, updatable = false)
  private String checkBank;

  @Column(name = "check_date", updatable = false)
  private LocalDate checkDate;

  @Column(name = "certificate_ref", length = 60, updatable = false)
  private String certificateRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private ReceiptSource source;

  @Column(name = "source_module", length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", length = 80, updatable = false)
  private String sourceRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReceiptStatus status = ReceiptStatus.ISSUED;

  @Column(name = "reinstated_amount", precision = 19, scale = 2)
  private BigDecimal reinstatedAmount;

  @Column(name = "printed_count", nullable = false)
  private int printedCount;

  @Column(name = "last_printed_at")
  private Instant lastPrintedAt;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(length = 250)
  private String remarks;

  @ElementCollection
  @CollectionTable(name = "csh_receipt_line", joinColumns = @JoinColumn(name = "receipt_id"))
  @OrderColumn(name = "line_no")
  private final List<ReceiptLine> lines = new ArrayList<>();

  protected Receipt() {}

  /**
   * Creates an issued receipt.
   *
   * @param header company, branch, number, kind, class, series, date and parties
   * @param money currency, BOOK rate and amounts
   * @param tender mode of payment, check, certificate and source
   */
  public Receipt(ReceiptHeader header, ReceiptMoney money, ReceiptTender tender) {
    this.companyId = header.companyId();
    this.branchId = header.branchId();
    this.receiptNo = header.receiptNo();
    this.kind = header.kind();
    this.receiptClass = header.receiptClass();
    this.seriesId = header.seriesId();
    this.receiptDate = header.receiptDate();
    this.payorCode = header.payorCode();
    this.payorName = header.payorName();
    this.assuredName = header.assuredName();
    this.salesUnit = header.salesUnit();
    this.currency = money.currency();
    this.bookRate = money.bookRate();
    this.amount = money.amount();
    this.baseAmount = money.baseAmount();
    this.gross = money.taxes().gross();
    this.vat = money.taxes().vat();
    this.wtax = money.taxes().wtax();
    this.mode = tender.mode();
    this.checkNo = tender.checkNo();
    this.checkBank = tender.checkBank();
    this.checkDate = tender.checkDate();
    this.certificateRef = tender.certificateRef();
    this.source = tender.source();
    this.sourceModule = tender.sourceModule();
    this.sourceRef = tender.sourceRef();
    this.remarks = tender.remarks();
  }

  /**
   * Adds an OR line.
   *
   * @param line line
   */
  public void addLine(ReceiptLine line) {
    lines.add(line);
  }

  /**
   * Records the journal of the issuance.
   *
   * @param batchNo journal batch
   */
  public void posted(String batchNo) {
    this.journalBatchNo = batchNo;
  }

  /**
   * Adds (or with a negative amount, removes) applied money.
   *
   * @param delta signed amount
   */
  public void applied(BigDecimal delta) {
    appliedAmount = appliedAmount.add(delta);
  }

  /**
   * Money of the receipt not applied to invoices.
   *
   * @return live amount - applied
   */
  public BigDecimal unappliedAmount() {
    return liveAmount().subtract(appliedAmount);
  }

  /**
   * The amount currently in force: nothing when cancelled, the reinstated amount when reinstated.
   *
   * @return live amount
   */
  public BigDecimal liveAmount() {
    return switch (status) {
      case ISSUED -> amount;
      case CANCELLED -> BigDecimal.ZERO;
      case REINSTATED -> reinstatedAmount;
    };
  }

  /** Cancels the receipt (CSHID.001/012). */
  public void cancel() {
    if (status == ReceiptStatus.CANCELLED) {
      throw new BusinessRuleException(
          "RECEIPT_ALREADY_CANCELLED", "Receipt " + receiptNo + " is already cancelled");
    }
    status = ReceiptStatus.CANCELLED;
  }

  /**
   * Reinstates a cancelled receipt in full or in part (CSHID.001/013).
   *
   * @param reinstated amount reinstated
   */
  public void reinstate(BigDecimal reinstated) {
    if (status != ReceiptStatus.CANCELLED) {
      throw new BusinessRuleException(
          "RECEIPT_NOT_CANCELLED", "Only a cancelled receipt can be reinstated");
    }
    if (reinstated.signum() <= 0 || reinstated.compareTo(amount) > 0) {
      throw new BusinessRuleException(
          "REINSTATEMENT_AMOUNT", "The reinstated amount must be above zero and at most " + amount);
    }
    status = ReceiptStatus.REINSTATED;
    reinstatedAmount = reinstated;
  }

  /**
   * Records a print (CSHID.019).
   *
   * @param at time
   */
  public void printed(Instant at) {
    printedCount++;
    lastPrintedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public ReceiptKind getKind() {
    return kind;
  }

  public String getReceiptClass() {
    return receiptClass;
  }

  public Long getSeriesId() {
    return seriesId;
  }

  public LocalDate getReceiptDate() {
    return receiptDate;
  }

  public String getPayorCode() {
    return payorCode;
  }

  public String getPayorName() {
    return payorName;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getSalesUnit() {
    return salesUnit;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getBookRate() {
    return bookRate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public BigDecimal getGross() {
    return gross;
  }

  public BigDecimal getVat() {
    return vat;
  }

  public BigDecimal getWtax() {
    return wtax;
  }

  public BigDecimal getAppliedAmount() {
    return appliedAmount;
  }

  public PaymentMode getMode() {
    return mode;
  }

  public String getCheckNo() {
    return checkNo;
  }

  public String getCheckBank() {
    return checkBank;
  }

  public LocalDate getCheckDate() {
    return checkDate;
  }

  public String getCertificateRef() {
    return certificateRef;
  }

  public ReceiptSource getSource() {
    return source;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public ReceiptStatus getStatus() {
    return status;
  }

  public BigDecimal getReinstatedAmount() {
    return reinstatedAmount;
  }

  public int getPrintedCount() {
    return printedCount;
  }

  public Instant getLastPrintedAt() {
    return lastPrintedAt;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getRemarks() {
    return remarks;
  }

  public List<ReceiptLine> getLines() {
    return lines;
  }

  /**
   * Identity of a receipt.
   *
   * @param companyId company
   * @param branchId issuing branch
   * @param receiptNo AR or OR number
   * @param kind AR or OR
   * @param receiptClass AR class (LOV AR_CLASS) or OR type (LOV OR_TYPE)
   * @param seriesId series the number came from
   * @param receiptDate receipt date
   * @param payorCode payor party code (client or insurer), may be null
   * @param payorName payor name
   * @param assuredName assured, may be null
   * @param salesUnit marketing unit, may be null
   */
  public record ReceiptHeader(
      Long companyId,
      Long branchId,
      String receiptNo,
      ReceiptKind kind,
      String receiptClass,
      Long seriesId,
      LocalDate receiptDate,
      String payorCode,
      String payorName,
      String assuredName,
      String salesUnit) {}

  /**
   * Money of a receipt.
   *
   * @param currency currency received
   * @param bookRate Comptrollership BOOK rate, 2 decimals (CSHID.012-014)
   * @param amount amount received (net for an OR)
   * @param baseAmount amount in the base currency
   * @param taxes gross, VAT and withholding tax (OR); zero for an AR
   */
  public record ReceiptMoney(
      String currency,
      BigDecimal bookRate,
      BigDecimal amount,
      BigDecimal baseAmount,
      OrAmounts taxes) {}

  /**
   * How the money was tendered and where the receipt comes from.
   *
   * @param mode mode of payment
   * @param checkNo check number, may be null
   * @param checkBank bank, may be null
   * @param checkDate check date, may be null
   * @param certificateRef BIR certificate reference, may be null
   * @param source receipt source
   * @param sourceModule requesting module (idempotency), may be null
   * @param sourceRef requesting reference (idempotency), may be null
   * @param remarks remarks
   */
  public record ReceiptTender(
      PaymentMode mode,
      String checkNo,
      String checkBank,
      LocalDate checkDate,
      String certificateRef,
      ReceiptSource source,
      String sourceModule,
      String sourceRef,
      String remarks) {}
}
