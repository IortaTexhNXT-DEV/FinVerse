package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reinsurance allocation of one premium transaction (policy issue or endorsement): per insured
 * risk, how the company's sum insured and net premium are split between retention, quota share,
 * surplus and facultative, and each treaty participant's share. Exactly one cession exists per
 * transaction (idempotency key: company, policy, endorsement number). Cessions are immutable once
 * posted.
 */
@Entity
@Table(name = "ri_cession")
public class Cession extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "cession_no", nullable = false, updatable = false, length = 40)
  private String cessionNo;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "policy_no", nullable = false, updatable = false, length = 40)
  private String policyNo;

  @Column(name = "endorsement_no", nullable = false, updatable = false)
  private int endorsementNo;

  @Column(name = "document_no", nullable = false, updatable = false, length = 50)
  private String documentNo;

  @Column(nullable = false, updatable = false, length = 20)
  private String kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 10)
  private CessionBasis basis;

  @Column(name = "business_line", nullable = false, updatable = false, length = 20)
  private String businessLine;

  @Column(name = "product_code", nullable = false, updatable = false, length = 20)
  private String productCode;

  @Column(name = "uw_year", nullable = false, updatable = false)
  private int uwYear;

  @Column(name = "treaty_year", nullable = false, updatable = false)
  private int treatyYear;

  @Column(name = "issue_date", nullable = false, updatable = false)
  private LocalDate issueDate;

  @Column(name = "effective_date", nullable = false, updatable = false)
  private LocalDate effectiveDate;

  @Column(name = "ri_date", nullable = false, updatable = false)
  private LocalDate riDate;

  @Column(nullable = false, updatable = false, length = 3)
  private String currency;

  @Column(name = "exchange_rate", nullable = false, updatable = false, precision = 19, scale = 8)
  private BigDecimal exchangeRate;

  @Column(name = "share_pct", nullable = false, updatable = false, precision = 19, scale = 8)
  private BigDecimal sharePct;

  @Column(name = "our_si", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal ourSi;

  @Column(name = "our_premium", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal ourPremium;

  @OneToMany(mappedBy = "cession", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("riskLineNo, layer, id")
  private final List<CessionLine> lines = new ArrayList<>();

  /** For JPA. */
  protected Cession() {}

  /**
   * Creates a cession.
   *
   * @param cessionNo cession number
   * @param basis allocation basis
   * @param h transaction identification and totals
   */
  public Cession(String cessionNo, CessionBasis basis, CessionHeader h) {
    this.cessionNo = cessionNo;
    this.basis = basis;
    this.companyId = h.companyId();
    this.branchId = h.branchId();
    this.policyId = h.policyId();
    this.policyNo = h.policyNo();
    this.endorsementNo = h.endorsementNo();
    this.documentNo = h.documentNo();
    this.kind = h.kind();
    this.businessLine = h.businessLine();
    this.productCode = h.productCode();
    this.uwYear = h.uwYear();
    this.treatyYear = h.treatyYear();
    this.issueDate = h.issueDate();
    this.effectiveDate = h.effectiveDate();
    this.riDate = h.riDate();
    this.currency = h.currency();
    this.exchangeRate = h.exchangeRate();
    this.sharePct = h.sharePct();
    this.ourSi = Money.round(h.ourSi());
    this.ourPremium = Money.round(h.ourPremium());
  }

  /**
   * Adds an allocation line.
   *
   * @param line line (belongs to this cession)
   */
  public void addLine(CessionLine line) {
    lines.add(line);
  }

  /**
   * Converts a policy-currency amount to the base currency at the cession rate.
   *
   * @param amount amount in the policy currency
   * @return base currency amount
   */
  public BigDecimal toBase(BigDecimal amount) {
    return Money.convert(amount, exchangeRate);
  }

  /**
   * Premium of the lines of a layer.
   *
   * @param layer layer
   * @return premium in the policy currency
   */
  public BigDecimal premiumOf(RiLayer layer) {
    return lines.stream()
        .filter(l -> l.getLayer() == layer)
        .map(CessionLine::getPremium)
        .reduce(Money.zero(), BigDecimal::add);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getCessionNo() {
    return cessionNo;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public int getEndorsementNo() {
    return endorsementNo;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public String getKind() {
    return kind;
  }

  public CessionBasis getBasis() {
    return basis;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public String getProductCode() {
    return productCode;
  }

  public int getUwYear() {
    return uwYear;
  }

  public int getTreatyYear() {
    return treatyYear;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  public LocalDate getRiDate() {
    return riDate;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getExchangeRate() {
    return exchangeRate;
  }

  public BigDecimal getSharePct() {
    return sharePct;
  }

  public BigDecimal getOurSi() {
    return ourSi;
  }

  public BigDecimal getOurPremium() {
    return ourPremium;
  }

  public List<CessionLine> getLines() {
    return List.copyOf(lines);
  }
}
