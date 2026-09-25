package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A booked invoice (BRNB.027/061): the broker's billing of one transaction of an account - the
 * original booking (one per policy year of a multi-year account, BRNB.112), a financial endorsement
 * or a cancellation. It carries the premium by component, the commission with VAT and withholding
 * tax, the insurer shares, the flags read by Operations and, once booked, its BIR-sequential
 * number, journal batches and open items.
 */
@Entity
@Table(name = "bkg_invoice")
public class BookedInvoice extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(name = "transaction_no", nullable = false, length = 40, updatable = false)
  private String transactionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private InvoiceKind kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InvoiceStatus status = InvoiceStatus.SCHEDULED;

  @Column(name = "endorsement_no", length = 40)
  private String endorsementNo;

  @Column(name = "parent_invoice_no", length = 40)
  private String parentInvoiceNo;

  @Column(name = "root_invoice_no", length = 40)
  private String rootInvoiceNo;

  @Column(name = "policy_year", nullable = false)
  private int policyYear;

  @Column(name = "policy_no", length = 60)
  private String policyNo;

  @Embedded private InvoiceFacts facts;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "booking_date")
  private LocalDate bookingDate;

  @Column(name = "inception_date", nullable = false)
  private LocalDate inceptionDate;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Embedded private PremiumComponents premium;

  @Embedded private CommissionTerms commission;

  @Embedded private InvoiceFlags flags;

  @Enumerated(EnumType.STRING)
  @Column(name = "booking_source", nullable = false, length = 20)
  private BookingSource source = BookingSource.INDIVIDUAL;

  @Column(name = "service_invoice_no", length = 40)
  private String serviceInvoiceNo;

  @Column(name = "booked_by", length = 50)
  private String bookedBy;

  @Column(name = "booked_at")
  private Instant bookedAt;

  @ElementCollection
  @CollectionTable(name = "bkg_invoice_share", joinColumns = @JoinColumn(name = "invoice_id"))
  @OrderColumn(name = "share_index")
  private final List<InsurerShare> shares = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "bkg_invoice_journal", joinColumns = @JoinColumn(name = "invoice_id"))
  @OrderColumn(name = "journal_index")
  @Column(name = "batch_no", nullable = false, length = 40)
  private final List<String> journalBatches = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "bkg_invoice_open_item", joinColumns = @JoinColumn(name = "invoice_id"))
  @OrderColumn(name = "item_index")
  private final List<InvoiceOpenItem> openItems = new ArrayList<>();

  protected BookedInvoice() {}

  private BookedInvoice(InvoiceDraft d) {
    this.companyId = d.companyId();
    this.branchId = d.branchId();
    this.arn = d.arn();
    this.accountId = d.accountId();
    this.transactionNo = d.transactionNo();
    this.kind = d.kind();
    this.policyYear = d.policyYear();
    this.policyNo = d.policyNo();
    this.facts = d.facts();
    this.currency = d.currency();
    this.inceptionDate = d.inceptionDate();
    this.expiryDate = d.expiryDate();
    this.premium = d.premium();
    this.commission = d.commission();
    this.flags = d.flags();
    this.endorsementNo = d.endorsementNo();
    this.parentInvoiceNo = d.parentInvoiceNo();
    this.shares.addAll(d.shares());
  }

  /**
   * A new invoice not yet booked (a scheduled policy year, or a draft about to be booked).
   *
   * @param draft invoice data
   * @return invoice in status SCHEDULED
   */
  public static BookedInvoice draft(InvoiceDraft draft) {
    return new BookedInvoice(draft);
  }

  /**
   * Books the invoice: number, date and who booked it (the posting follows in the same
   * transaction).
   *
   * @param number BIR-sequential invoice number
   * @param date booking date
   * @param bookingSource how it was booked
   * @param user user (or SYSTEM)
   * @param when time
   */
  public void book(
      String number, LocalDate date, BookingSource bookingSource, String user, Instant when) {
    if (status != InvoiceStatus.SCHEDULED) {
      throw new BusinessRuleException(
          "INVOICE_ALREADY_BOOKED", "Invoice " + arn + "/" + transactionNo + " is " + status);
    }
    this.invoiceNo = number;
    // Endorsements and cancellations always name the original booking as parent (DIS 3.27.2)
    this.rootInvoiceNo = parentInvoiceNo == null ? number : parentInvoiceNo;
    this.bookingDate = date;
    this.source = bookingSource;
    this.bookedBy = user;
    this.bookedAt = when;
    this.status = InvoiceStatus.BOOKED;
  }

  /**
   * Records the journal batches and open items of the posting.
   *
   * @param batches journal batch numbers
   * @param items open items recorded
   */
  public void recordPosting(List<String> batches, List<InvoiceOpenItem> items) {
    journalBatches.addAll(batches);
    openItems.addAll(items);
  }

  /**
   * Links the service invoice issued for this invoice (BRNB.100).
   *
   * @param number service invoice number
   */
  public void linkServiceInvoice(String number) {
    this.serviceInvoiceNo = number;
  }

  /** A scheduled policy year of a cancelled account will not be booked. */
  public void cancelSchedule() {
    if (status == InvoiceStatus.SCHEDULED) {
      this.status = InvoiceStatus.CANCELLED;
    }
  }

  /**
   * Replaces the cost center of a scheduled invoice before it is booked (BRNB.108).
   *
   * @param newFacts facts with the cost center to use
   */
  public void replaceFacts(InvoiceFacts newFacts) {
    if (status == InvoiceStatus.SCHEDULED) {
      this.facts = newFacts;
    }
  }

  /**
   * Whether this invoice is booked.
   *
   * @return true when BOOKED
   */
  public boolean isBooked() {
    return status == InvoiceStatus.BOOKED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getArn() {
    return arn;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getTransactionNo() {
    return transactionNo;
  }

  public InvoiceKind getKind() {
    return kind;
  }

  public InvoiceStatus getStatus() {
    return status;
  }

  public String getEndorsementNo() {
    return endorsementNo;
  }

  public String getParentInvoiceNo() {
    return parentInvoiceNo;
  }

  /**
   * Root of the invoice family: the invoice itself for an original booking, else the root of its
   * parent (DIS 3.27.2); the same value as the Operations ledger's {@code root_invoice_no}.
   *
   * @return root invoice number, null before booking
   */
  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public InvoiceFacts getFacts() {
    return facts;
  }

  public String getCurrency() {
    return currency;
  }

  public LocalDate getBookingDate() {
    return bookingDate;
  }

  public LocalDate getInceptionDate() {
    return inceptionDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public PremiumComponents getPremium() {
    return premium;
  }

  public CommissionTerms getCommission() {
    return commission;
  }

  public InvoiceFlags getFlags() {
    return flags;
  }

  public BookingSource getSource() {
    return source;
  }

  public String getServiceInvoiceNo() {
    return serviceInvoiceNo;
  }

  public String getBookedBy() {
    return bookedBy;
  }

  public Instant getBookedAt() {
    return bookedAt;
  }

  public List<InsurerShare> getShares() {
    return List.copyOf(shares);
  }

  public List<String> getJournalBatches() {
    return List.copyOf(journalBatches);
  }

  public List<InvoiceOpenItem> getOpenItems() {
    return List.copyOf(openItems);
  }
}
