package com.iortatechnxt.brokerverse.account.domain;

import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An account: the risk record to be placed with an insurer, one per policy, identified by its
 * Account Reference Number from quotation to invoice (BRNB.102). Its status mirrors the NB_ACCOUNT
 * work case; lifecycle data is written by later modules through {@code AccountLifecycleService}.
 */
@Entity
@Table(name = "acc_account")
public class Account extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "quotation_ref", length = 40, updatable = false)
  private String quotationRef;

  @Column(name = "proposal_ref", length = 40, updatable = false)
  private String proposalRef;

  @Column(name = "client_id", nullable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30)
  private String clientCode;

  @Column(name = "client_name", nullable = false, length = 250)
  private String clientName;

  @Column(name = "product_code", nullable = false, length = 20)
  private String productCode;

  @Column(name = "line_code", nullable = false, length = 30)
  private String lineCode;

  @Column(name = "cover_type_code", length = 30)
  private String coverTypeCode;

  @Column(name = "market_segment", length = 40)
  private String marketSegment;

  @Column(name = "source_channel", length = 40)
  private String sourceChannel;

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Column(name = "insurer_branch", length = 20)
  private String insurerBranch;

  @Column(name = "period_from")
  private LocalDate periodFrom;

  @Column(name = "period_to")
  private LocalDate periodTo;

  @Column(name = "multi_year", nullable = false)
  private boolean multiYear;

  @Column(name = "term_years", nullable = false)
  private int termYears = 1;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "total_sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalSumInsured = BigDecimal.ZERO;

  @Embedded private AccountPremium premium;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_arrangement", nullable = false, length = 20)
  private PaymentArrangement paymentArrangement = PaymentArrangement.VIA_BDOI;

  @Column(name = "direct_payment_tagged_by", length = 50)
  private String directPaymentTaggedBy;

  @Column(name = "direct_payment_tagged_at")
  private Instant directPaymentTaggedAt;

  @Column(name = "mortgagee_bank", length = 40)
  private String mortgageeBank;

  @Column(name = "loan_application_no", length = 40)
  private String loanApplicationNo;

  @ElementCollection
  @CollectionTable(name = "acc_account_pn", joinColumns = @JoinColumn(name = "account_id"))
  @OrderColumn(name = "pn_index")
  @Column(name = "pn_number", nullable = false, length = 40)
  private final List<String> pnNumbers = new ArrayList<>();

  @Embedded private FreeFirstYear freeFirstYear;

  @Embedded private AccountContact contact;

  @Embedded private SalesStamp sales;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AccountStatus status = AccountStatus.DRAFT;

  @Embedded private TsuClearance tsu;

  @Column(name = "direct_booking", nullable = false)
  private boolean directBooking;

  @Embedded private final AccountLifecycle lifecycle = new AccountLifecycle();

  @ElementCollection
  @CollectionTable(name = "acc_account_policy", joinColumns = @JoinColumn(name = "account_id"))
  @OrderColumn(name = "year_index")
  @Column(name = "policy_number", nullable = false, length = 60)
  private final List<String> policyNumbers = new ArrayList<>();

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("itemNo")
  private final List<RiskItem> items = new ArrayList<>();

  protected Account() {}

  private Account(Long companyId, String arn, Origin origin, SalesStamp salesStamp) {
    this.companyId = companyId;
    this.arn = arn;
    this.quotationRef = origin.quotationRef();
    this.proposalRef = origin.proposalRef();
    this.sales = salesStamp;
    this.freeFirstYear = FreeFirstYear.NONE;
    this.tsu = TsuClearance.NONE;
    this.premium = AccountPremium.NONE;
  }

  /**
   * Creates a draft account.
   *
   * @param companyId company
   * @param arn Account Reference Number
   * @param origin quotation and proposal references
   * @param data account data
   * @param salesStamp sales unit and cost center
   * @return the account (not yet saved)
   */
  public static Account create(
      Long companyId, String arn, Origin origin, AccountData data, SalesStamp salesStamp) {
    Account account = new Account(companyId, arn, origin, salesStamp);
    account.apply(data);
    return account;
  }

  /**
   * Replaces the maintainable data; items are updated in place so their numbers stay stable.
   *
   * @param data account data
   */
  public void apply(AccountData data) {
    if (data.multiYear() && data.termYears() < 2) {
      throw new BusinessRuleException(
          "ACCOUNT_TERM_INVALID", "A multi-year account needs a term of at least 2 years");
    }
    this.clientId = data.client().id();
    this.clientCode = data.client().code();
    this.clientName = data.client().name();
    this.productCode = data.product().code();
    this.lineCode = data.product().lineCode();
    this.coverTypeCode = data.product().coverTypeCode();
    this.marketSegment = data.marketSegment();
    this.sourceChannel = data.sourceChannel();
    this.insurerCode = data.insurerCode();
    this.insurerBranch = data.insurerBranch();
    this.periodFrom = data.periodFrom();
    this.periodTo = data.periodTo();
    this.multiYear = data.multiYear();
    this.termYears = data.multiYear() ? data.termYears() : 1;
    this.currency = data.currency() == null ? "PHP" : data.currency();
    applyMortgage(data.mortgage() == null ? Mortgage.NONE : data.mortgage());
    this.contact = data.contact() == null ? AccountContact.NONE : data.contact();
    applyItems(data);
  }

  private void applyMortgage(Mortgage mortgage) {
    this.mortgageeBank = mortgage.bank();
    this.loanApplicationNo = mortgage.loanApplicationNo();
    this.pnNumbers.clear();
    mortgage.pnNumbers().stream()
        .filter(pn -> pn != null && !pn.isBlank())
        .map(String::strip)
        .forEach(pnNumbers::add);
  }

  private void applyItems(AccountData data) {
    List<RiskItemData> incoming = data.items();
    for (int i = 0; i < incoming.size(); i++) {
      if (i < items.size()) {
        items.get(i).update(data.product().itemKind(), incoming.get(i));
      } else {
        items.add(new RiskItem(this, i + 1, data.product().itemKind(), incoming.get(i)));
      }
    }
    while (items.size() > incoming.size()) {
      items.remove(items.size() - 1);
    }
    this.totalSumInsured =
        items.stream()
            .map(RiskItem::getSumInsured)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Stores the premium computed by the calculator (null to clear it).
   *
   * @param newPremium breakdown
   */
  public void setPremium(AccountPremium newPremium) {
    this.premium = newPremium == null ? AccountPremium.NONE : newPremium;
  }

  /**
   * Sets the payment arrangement; a change to direct payment is stamped (BRNB.114).
   *
   * @param arrangement arrangement
   * @param user user
   * @param when time
   */
  public void setPaymentArrangement(PaymentArrangement arrangement, String user, Instant when) {
    PaymentArrangement value = arrangement == null ? PaymentArrangement.VIA_BDOI : arrangement;
    if (value != paymentArrangement) {
      this.directPaymentTaggedBy = value == PaymentArrangement.DIRECT_TO_INSURER ? user : null;
      this.directPaymentTaggedAt = value == PaymentArrangement.DIRECT_TO_INSURER ? when : null;
    }
    this.paymentArrangement = value;
  }

  /**
   * Mirrors the work case stage.
   *
   * @param newStatus status
   */
  public void markStatus(AccountStatus newStatus) {
    this.status = newStatus;
  }

  /**
   * Sets or cancels the Free First Year tag (BRNB.113).
   *
   * @param ffy tag
   */
  public void setFreeFirstYear(FreeFirstYear ffy) {
    this.freeFirstYear = ffy == null ? FreeFirstYear.NONE : ffy;
  }

  /**
   * Records the TSU routing outcome, keeping an earlier clearance.
   *
   * @param required whether TSU clearance is required
   * @param rule matching rule
   */
  public void setTsuRequirement(boolean required, String rule) {
    TsuClearance current = getTsu();
    this.tsu = new TsuClearance(required, rule, current.clearedBy(), current.clearedAt());
  }

  /**
   * Records the TSU clearance.
   *
   * @param user TSU user
   * @param when time
   */
  public void clearTsu(String user, Instant when) {
    this.tsu = new TsuClearance(getTsu().required(), getTsu().rule(), user, when);
  }

  /** Marks the account as booked directly (policy already issued, BRNB.111). */
  public void markDirectBooking() {
    this.directBooking = true;
  }

  /**
   * Changes the cost center stamped on the account (booking, BRNB.108).
   *
   * @param costCenter cost center
   */
  public void setCostCenter(String costCenter) {
    this.sales = getSales().withCostCenter(costCenter);
  }

  /**
   * Records the payment gate outcome.
   *
   * @param paymentStatus status
   * @param source source
   * @param when time
   */
  public void recordPayment(PaymentStatus paymentStatus, String source, Instant when) {
    lifecycle.paymentConfirmed(paymentStatus, source, when);
  }

  /**
   * Records the placement slip.
   *
   * @param slipRef placement slip number
   * @param when time
   */
  public void recordPlacement(String slipRef, Instant when) {
    lifecycle.placed(slipRef, when);
  }

  /**
   * Records the hold cover.
   *
   * @param holdCoverStatus status
   * @param reference insurer reference
   * @param date date
   */
  public void recordHoldCover(HoldCoverStatus holdCoverStatus, String reference, LocalDate date) {
    lifecycle.holdCover(holdCoverStatus, reference, date);
  }

  /**
   * Records the policy numbers (one per policy year, BRNB.112) and the issue date.
   *
   * @param numbers policy numbers
   * @param issueDate issue date
   */
  public void recordPolicies(List<String> numbers, LocalDate issueDate) {
    this.policyNumbers.clear();
    this.policyNumbers.addAll(numbers);
    lifecycle.policyIssued(issueDate, true);
  }

  /**
   * Records the booking.
   *
   * @param reference booking reference
   * @param date booking date
   * @param incentive incentive flag (BRNB.107)
   */
  public void recordBooking(String reference, LocalDate date, boolean incentive) {
    lifecycle.booked(reference, date, incentive);
  }

  /**
   * Records a cancellation after issuance (BRNB.094).
   *
   * @param date cancellation date
   * @param reason reason
   */
  public void recordCancellation(LocalDate date, String reason) {
    lifecycle.cancelled(date, reason);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getArn() {
    return arn;
  }

  public String getQuotationRef() {
    return quotationRef;
  }

  public String getProposalRef() {
    return proposalRef;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getClientName() {
    return clientName;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getCoverTypeCode() {
    return coverTypeCode;
  }

  public String getMarketSegment() {
    return marketSegment;
  }

  public String getSourceChannel() {
    return sourceChannel;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerBranch() {
    return insurerBranch;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public boolean isMultiYear() {
    return multiYear;
  }

  public int getTermYears() {
    return termYears;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getTotalSumInsured() {
    return totalSumInsured;
  }

  public AccountPremium getPremium() {
    return premium == null ? AccountPremium.NONE : premium;
  }

  public PaymentArrangement getPaymentArrangement() {
    return paymentArrangement;
  }

  /**
   * Direct payment flag (BRNB.114): the client pays the insurer directly.
   *
   * @return true for direct payment
   */
  public boolean isDirectPayment() {
    return paymentArrangement == PaymentArrangement.DIRECT_TO_INSURER;
  }

  public String getDirectPaymentTaggedBy() {
    return directPaymentTaggedBy;
  }

  public Instant getDirectPaymentTaggedAt() {
    return directPaymentTaggedAt;
  }

  public String getMortgageeBank() {
    return mortgageeBank;
  }

  public String getLoanApplicationNo() {
    return loanApplicationNo;
  }

  public List<String> getPnNumbers() {
    return List.copyOf(pnNumbers);
  }

  public FreeFirstYear getFreeFirstYear() {
    return freeFirstYear == null ? FreeFirstYear.NONE : freeFirstYear;
  }

  public AccountContact getContact() {
    return contact == null ? AccountContact.NONE : contact;
  }

  public SalesStamp getSales() {
    return sales == null ? new SalesStamp(null, null, null, null, null) : sales;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public TsuClearance getTsu() {
    return tsu == null ? TsuClearance.NONE : tsu;
  }

  public boolean isDirectBooking() {
    return directBooking;
  }

  public AccountLifecycle getLifecycle() {
    return lifecycle;
  }

  public List<String> getPolicyNumbers() {
    return List.copyOf(policyNumbers);
  }

  public List<RiskItem> getItems() {
    return List.copyOf(items);
  }

  /**
   * Where an account comes from (plain references, BRNB.102).
   *
   * @param quotationRef quotation number
   * @param proposalRef proposal request (PRF) number
   */
  public record Origin(String quotationRef, String proposalRef) {

    /** Created directly. */
    public static final Origin DIRECT = new Origin(null, null);
  }
}
