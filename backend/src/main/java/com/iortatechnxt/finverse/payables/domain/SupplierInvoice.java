package com.iortatechnxt.finverse.payables.domain;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Supplier invoice (accounts payable document).
 *
 * <p>Lifecycle DRAFT → PENDING_APPROVAL → APPROVED, or CANCELLED before approval. On approval the
 * service publishes the accounting events and records the CREDIT open item of the supplier; the
 * invoice then becomes immutable.
 */
@Entity
@Table(name = "pay_supplier_invoice")
public class SupplierInvoice extends SubmittableDocument {

  private static final Set<InvoiceStatus> EDITABLE = EnumSet.of(InvoiceStatus.DRAFT);

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "document_no", nullable = false, length = 40)
  private String documentNo;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "party_code", nullable = false, length = 30)
  private String partyCode;

  @Column(name = "supplier_invoice_no", nullable = false, length = 40)
  private String supplierInvoiceNo;

  @Column(name = "invoice_date", nullable = false)
  private LocalDate invoiceDate;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "vat_applicable", nullable = false)
  private boolean vatApplicable;

  @Column(name = "wht_rate", nullable = false, precision = 7, scale = 4)
  private BigDecimal whtRate = BigDecimal.ZERO;

  @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal netAmount = BigDecimal.ZERO;

  @Column(name = "vat_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal vatAmount = BigDecimal.ZERO;

  @Column(name = "wht_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal whtAmount = BigDecimal.ZERO;

  @Column(name = "payable_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal payableAmount = BigDecimal.ZERO;

  @Column(name = "base_payable_amount", precision = 19, scale = 2)
  private BigDecimal basePayableAmount;

  @Column(length = 250)
  private String narration;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InvoiceStatus status = InvoiceStatus.DRAFT;

  @Column(name = "open_item_id")
  private Long openItemId;

  @Column(name = "journal_batch_nos", length = 400)
  private String journalBatchNos;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<SupplierInvoiceLine> lines = new ArrayList<>();

  protected SupplierInvoice() {}

  /**
   * Creates a draft invoice.
   *
   * @param documentNo internal document number
   * @param header header values
   */
  public SupplierInvoice(String documentNo, InvoiceHeader header) {
    this.documentNo = documentNo;
    this.companyId = header.companyId();
    this.partyId = header.partyId();
    this.partyCode = header.partyCode();
    applyHeader(header);
  }

  /**
   * Updates header values of a draft.
   *
   * @param header header values (company and supplier are immutable)
   */
  public void updateHeader(InvoiceHeader header) {
    requireStatus(EDITABLE, "edit");
    applyHeader(header);
  }

  /**
   * Removes all lines of a draft (flush before adding the new ones, so the unique line numbers are
   * free again).
   */
  public void clearLines() {
    requireStatus(EDITABLE, "edit");
    lines.clear();
  }

  /**
   * Replaces all lines of a draft and recomputes VAT, withholding and totals.
   *
   * @param values line values (at least one)
   */
  public void replaceLines(List<InvoiceLineValues> values) {
    requireStatus(EDITABLE, "edit");
    if (values.isEmpty()) {
      throw new BusinessRuleException(
          "INVOICE_WITHOUT_LINES", "An invoice needs at least one line");
    }
    lines.clear();
    int number = 1;
    for (InvoiceLineValues v : values) {
      BigDecimal vat = InvoiceCalculator.vat(v.netAmount(), vatApplicable);
      BigDecimal wht = InvoiceCalculator.withholding(v.netAmount(), whtRate);
      lines.add(new SupplierInvoiceLine(this, number++, v, vat, wht));
    }
    recalculate();
  }

  /**
   * Submits the draft for approval.
   *
   * @param user maker
   * @param when timestamp
   */
  public void submit(String user, Instant when) {
    requireStatus(EDITABLE, "submit");
    if (payableAmount.signum() <= 0) {
      throw new BusinessRuleException("INVALID_INVOICE_AMOUNT", "Amount payable must be positive");
    }
    recordSubmission(user, when);
    status = InvoiceStatus.PENDING_APPROVAL;
  }

  /**
   * Approves the invoice once posted.
   *
   * @param checker approving user
   * @param when timestamp
   * @param posting posting results
   */
  public void approve(String checker, Instant when, InvoicePosting posting) {
    requireStatus(EnumSet.of(InvoiceStatus.PENDING_APPROVAL), "approve");
    recordApproval(checker, when);
    this.openItemId = posting.openItemId();
    this.journalBatchNos = String.join(",", posting.batchNos());
    this.basePayableAmount = posting.basePayable();
    status = InvoiceStatus.APPROVED;
  }

  /**
   * Returns a submitted invoice to the maker.
   *
   * @param checker checker
   * @param reason reason
   */
  public void reject(String checker, String reason) {
    requireStatus(EnumSet.of(InvoiceStatus.PENDING_APPROVAL), "reject");
    requireChecker(checker);
    clearSubmission();
    recordReason(reason);
    status = InvoiceStatus.DRAFT;
  }

  /**
   * Cancels an invoice that was not approved.
   *
   * @param reason reason
   */
  public void cancel(String reason) {
    requireStatus(EnumSet.of(InvoiceStatus.DRAFT, InvoiceStatus.PENDING_APPROVAL), "cancel");
    recordReason(reason);
    status = InvoiceStatus.CANCELLED;
  }

  private void applyHeader(InvoiceHeader h) {
    if (h.dueDate().isBefore(h.invoiceDate())) {
      throw new BusinessRuleException("INVALID_DUE_DATE", "Due date is before the invoice date");
    }
    this.branchId = h.branchId();
    this.supplierInvoiceNo = h.supplierInvoiceNo();
    this.invoiceDate = h.invoiceDate();
    this.dueDate = h.dueDate();
    this.currency = h.currency();
    this.vatApplicable = h.vatApplicable();
    this.whtRate = h.whtRate() == null ? BigDecimal.ZERO : h.whtRate();
    this.narration = h.narration();
  }

  private void recalculate() {
    netAmount = sum(SupplierInvoiceLine::getNetAmount);
    vatAmount = sum(SupplierInvoiceLine::getVatAmount);
    whtAmount = sum(SupplierInvoiceLine::getWhtAmount);
    payableAmount = InvoiceCalculator.payable(netAmount, vatAmount, whtAmount);
  }

  private BigDecimal sum(Function<SupplierInvoiceLine, BigDecimal> field) {
    return Money.round(lines.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  private void requireStatus(Set<InvoiceStatus> allowed, String action) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(
          "INVALID_INVOICE_STATUS",
          "Cannot " + action + " invoice " + documentNo + " in " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public Long getPartyId() {
    return partyId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getSupplierInvoiceNo() {
    return supplierInvoiceNo;
  }

  public LocalDate getInvoiceDate() {
    return invoiceDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getCurrency() {
    return currency;
  }

  public boolean isVatApplicable() {
    return vatApplicable;
  }

  public BigDecimal getWhtRate() {
    return whtRate;
  }

  public BigDecimal getNetAmount() {
    return netAmount;
  }

  public BigDecimal getVatAmount() {
    return vatAmount;
  }

  public BigDecimal getWhtAmount() {
    return whtAmount;
  }

  public BigDecimal getPayableAmount() {
    return payableAmount;
  }

  public BigDecimal getBasePayableAmount() {
    return basePayableAmount;
  }

  public String getNarration() {
    return narration;
  }

  public InvoiceStatus getStatus() {
    return status;
  }

  public Long getOpenItemId() {
    return openItemId;
  }

  public String getJournalBatchNos() {
    return journalBatchNos;
  }

  public List<SupplierInvoiceLine> getLines() {
    return Collections.unmodifiableList(lines);
  }
}
