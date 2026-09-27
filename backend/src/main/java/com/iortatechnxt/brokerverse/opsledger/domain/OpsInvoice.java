package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;

/**
 * A booked invoice or endorsement invoice in the Operations ledger (OPERATIONS_DESIGN 4.1): header
 * facts, insurer shares (ADJID.027), components with balances, payment and remittance status
 * (RMTID.019/032), flags and the lock of the module working on it (RMTID.040).
 */
@Entity
@Table(name = "ops_invoice")
@SuppressWarnings("PMD.GodClass") // aggregate root of the ledger: header, flags, lock, balances
public class OpsInvoice extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "account_id", updatable = false)
  private Long accountId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private InvoiceKind kind;

  @Column(name = "endorsement_no", length = 40, updatable = false)
  private String endorsementNo;

  @Column(name = "parent_invoice_no", length = 40, updatable = false)
  private String parentInvoiceNo;

  @Column(name = "root_invoice_no", nullable = false, length = 40)
  private String rootInvoiceNo;

  @Column(name = "policy_no", length = 60)
  private String policyNo;

  @Column(name = "policy_year", nullable = false, updatable = false)
  private int policyYear;

  @Column(name = "pn_nos", length = 500)
  private String pnNos;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "assured_name", nullable = false, length = 250)
  private String assuredName;

  @Column(name = "payor_name", length = 250)
  private String payorName;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Embedded private OpsInvoiceData.Classification classification;

  @Column(name = "gross_premium", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal grossPremium;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal commission;

  @Column(
      name = "vat_on_commission",
      nullable = false,
      precision = 19,
      scale = 2,
      updatable = false)
  private BigDecimal vatOnCommission;

  @Column(name = "wtax_rate", nullable = false, precision = 9, scale = 4, updatable = false)
  private BigDecimal wtaxRate;

  @Column(name = "dp_flag", nullable = false, updatable = false)
  private boolean dpFlag;

  @Column(name = "cwt_flag", nullable = false)
  private boolean cwtFlag;

  @Column(name = "incentive_eligible", nullable = false, updatable = false)
  private boolean incentiveEligible;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false, length = 20)
  private PaymentStatus paymentStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "remittance_status", nullable = false, length = 30)
  private RemittanceStatus remittanceStatus;

  @Column(name = "hold_flag", nullable = false)
  private boolean holdFlag;

  @Column(name = "pending_neg_adj", nullable = false)
  private boolean pendingNegAdj;

  @Column(name = "written_off", nullable = false)
  private boolean writtenOff;

  @Column(nullable = false)
  private boolean cancelled;

  @Column(nullable = false)
  private boolean estimated;

  @Column(name = "lock_owner", length = 30)
  private String lockOwner;

  @Column(name = "lock_reason", length = 200)
  private String lockReason;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Column(name = "locked_by", length = 50)
  private String lockedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "feed_source", nullable = false, length = 20, updatable = false)
  private FeedSource feedSource;

  @ElementCollection
  @CollectionTable(name = "ops_invoice_share", joinColumns = @JoinColumn(name = "invoice_id"))
  @OrderColumn(name = "share_index")
  private final List<OpsInvoiceShare> shares = new ArrayList<>();

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<OpsInvoiceComponent> components = new ArrayList<>();

  protected OpsInvoice() {}

  /**
   * A booked invoice entering the ledger, with one component row per {@link LedgerComponent}.
   *
   * @param data header facts
   * @param shares insurer shares
   * @param source event or replay
   * @return the invoice (components still at zero; the feed posts the BOOKED movements)
   */
  public static OpsInvoice of(
      OpsInvoiceData data, List<OpsInvoiceShare> shares, FeedSource source) {
    OpsInvoice i = new OpsInvoice();
    i.keys(data.keys());
    i.parties(data.parties());
    i.classification = data.classification();
    i.grossPremium = data.amounts().grossPremium();
    i.commission = data.amounts().commission();
    i.vatOnCommission = data.amounts().vatOnCommission();
    i.wtaxRate = data.amounts().wtaxRate();
    i.dpFlag = data.flags().directPayment();
    i.cwtFlag = data.flags().cwt2Percent();
    i.incentiveEligible = data.flags().incentiveEligible();
    i.feedSource = source;
    i.shares.addAll(shares);
    for (LedgerComponent c : LedgerComponent.values()) {
      i.components.add(new OpsInvoiceComponent(i, c));
    }
    boolean receivable = !i.dpFlag && !i.kind.isNegative();
    i.paymentStatus = receivable ? PaymentStatus.UNPAID : PaymentStatus.NOT_APPLICABLE;
    if (i.dpFlag) {
      i.remittanceStatus = RemittanceStatus.NOT_APPLICABLE;
    } else {
      i.remittanceStatus =
          receivable ? RemittanceStatus.WITH_OUTSTANDING_BALANCE : RemittanceStatus.UNPROCESSED;
    }
    return i;
  }

  private void keys(OpsInvoiceData.Keys k) {
    this.companyId = k.companyId();
    this.branchId = k.branchId();
    this.invoiceNo = k.invoiceNo();
    this.arn = k.arn();
    this.accountId = k.accountId();
    this.kind = k.kind();
    this.endorsementNo = k.endorsementNo();
    this.parentInvoiceNo = k.parentInvoiceNo();
    this.rootInvoiceNo = k.rootInvoiceNo();
    this.policyNo = k.policyNo();
    this.policyYear = k.policyYear();
  }

  private void parties(OpsInvoiceData.Parties p) {
    this.clientCode = p.clientCode();
    this.assuredName = p.assuredName();
    this.payorName = p.payorName();
    this.insurerCode = p.insurerCode();
  }

  /**
   * Moves a component's balance and derives the payment status again.
   *
   * @param type movement type
   * @param component component
   * @param amount signed amount
   */
  public void move(MovementType type, LedgerComponent component, BigDecimal amount) {
    component(component).move(type.bucket(), amount);
    derivePaymentStatus();
  }

  private void derivePaymentStatus() {
    if (paymentStatus == PaymentStatus.NOT_APPLICABLE) {
      return;
    }
    paymentStatus = PaymentStatus.of(components);
    if (remittanceStatus.isDerived()) {
      remittanceStatus = RemittanceStatus.derivedFrom(paymentStatus);
    }
  }

  /**
   * Sets or clears a flag.
   *
   * @param flag flag
   * @param value new value
   * @return the previous value
   */
  public boolean setFlag(InvoiceFlag flag, boolean value) {
    boolean previous = flag(flag);
    switch (flag) {
      case HOLD -> holdFlag = value;
      case PENDING_NEG_ADJ -> pendingNegAdj = value;
      case WRITTEN_OFF -> writtenOff = value;
      case CANCELLED -> cancelled = value;
      default -> estimated = value;
    }
    return previous;
  }

  /**
   * A flag's value.
   *
   * @param flag flag
   * @return value
   */
  public boolean flag(InvoiceFlag flag) {
    return switch (flag) {
      case HOLD -> holdFlag;
      case PENDING_NEG_ADJ -> pendingNegAdj;
      case WRITTEN_OFF -> writtenOff;
      case CANCELLED -> cancelled;
      case ESTIMATED -> estimated;
    };
  }

  /**
   * Sets the remittance status (remittance module, RMTID.019).
   *
   * @param status new status
   * @return the previous status
   */
  public RemittanceStatus changeRemittanceStatus(RemittanceStatus status) {
    RemittanceStatus previous = remittanceStatus;
    remittanceStatus = status;
    if (status.isDerived()) {
      derivePaymentStatus();
    }
    return previous;
  }

  /**
   * Locks the invoice for a module (RMTID.040). Locking again by the same module refreshes the
   * reason.
   *
   * @param owner module taking the lock
   * @param reason why
   * @param by user
   * @param at time
   */
  public void lock(String owner, String reason, String by, Instant at) {
    requireNotLockedByOther(owner);
    lockOwner = owner;
    lockReason = reason;
    lockedBy = by;
    lockedAt = at;
  }

  /**
   * Releases the module's lock.
   *
   * @param owner module holding the lock
   * @return true when a lock was released
   */
  public boolean unlock(String owner) {
    if (lockOwner == null) {
      return false;
    }
    requireNotLockedByOther(owner);
    lockOwner = null;
    lockReason = null;
    lockedBy = null;
    lockedAt = null;
    return true;
  }

  /**
   * Refuses a change by a module while another module holds the lock (RMTID.040).
   *
   * @param module module that wants to change the invoice
   */
  public void requireNotLockedByOther(String module) {
    if (lockOwner != null && !lockOwner.equals(module)) {
      throw new BusinessRuleException(
          "INVOICE_LOCKED",
          "Invoice "
              + invoiceNo
              + " is locked by "
              + lockOwner
              + (lockReason == null ? "" : " (" + lockReason + ")"));
    }
  }

  /**
   * Updates the policy and PN numbers (endorsements, prodrecon feedback).
   *
   * @param policy policy number
   * @param pns PN numbers, comma separated
   */
  public void updateReferences(String policy, String pns) {
    this.policyNo = policy;
    this.pnNos = pns;
  }

  /**
   * One component.
   *
   * @param component component
   * @return the row
   */
  public OpsInvoiceComponent component(LedgerComponent component) {
    return components.stream()
        .filter(c -> c.getComponent() == component)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Missing component " + component));
  }

  /**
   * Outstanding balance of the client's premium receivable.
   *
   * @return sum of the PR component balances
   */
  public BigDecimal premiumBalance() {
    return components.stream()
        .filter(c -> c.getComponent().isPremiumReceivable())
        .map(OpsInvoiceComponent::getBalance)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Balances of every component.
   *
   * @return balance per component
   */
  public Map<LedgerComponent, BigDecimal> balances() {
    Map<LedgerComponent, BigDecimal> map = new EnumMap<>(LedgerComponent.class);
    components.forEach(c -> map.put(c.getComponent(), c.getBalance()));
    return map;
  }

  /** Loads the components and shares (reads outside the persistence context). */
  public void loadCollections() {
    Hibernate.initialize(components);
    Hibernate.initialize(shares);
  }

  /**
   * The lead insurer's share.
   *
   * @return lead share, else the first share
   */
  public OpsInvoiceShare leadShare() {
    return shares.stream().filter(OpsInvoiceShare::lead).findFirst().orElse(shares.get(0));
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

  public InvoiceKind getKind() {
    return kind;
  }

  public String getEndorsementNo() {
    return endorsementNo;
  }

  public String getParentInvoiceNo() {
    return parentInvoiceNo;
  }

  /**
   * Root of the invoice family (DIS 3.27.2, ACSL 2.16.0): the invoice's own number for an original
   * booking, the first invoice of the parent chain for endorsements and cancellations.
   *
   * @return root invoice number
   */
  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public String getPnNos() {
    return pnNos;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getPayorName() {
    return payorName;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public OpsInvoiceData.Classification getClassification() {
    return classification;
  }

  public String getCurrency() {
    return classification.currency();
  }

  public LocalDate getBookingDate() {
    return classification.bookingDate();
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public BigDecimal getVatOnCommission() {
    return vatOnCommission;
  }

  public BigDecimal getWtaxRate() {
    return wtaxRate;
  }

  public boolean isDpFlag() {
    return dpFlag;
  }

  public boolean isCwtFlag() {
    return cwtFlag;
  }

  public boolean isIncentiveEligible() {
    return incentiveEligible;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  public RemittanceStatus getRemittanceStatus() {
    return remittanceStatus;
  }

  public boolean isHoldFlag() {
    return holdFlag;
  }

  public boolean isPendingNegAdj() {
    return pendingNegAdj;
  }

  public boolean isWrittenOff() {
    return writtenOff;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public boolean isEstimated() {
    return estimated;
  }

  public String getLockOwner() {
    return lockOwner;
  }

  public String getLockReason() {
    return lockReason;
  }

  public Instant getLockedAt() {
    return lockedAt;
  }

  public String getLockedBy() {
    return lockedBy;
  }

  public FeedSource getFeedSource() {
    return feedSource;
  }

  public List<OpsInvoiceShare> getShares() {
    return Collections.unmodifiableList(shares);
  }

  public List<OpsInvoiceComponent> getComponents() {
    return Collections.unmodifiableList(components);
  }
}
