package com.iortatechnxt.brokerverse.consolidation.domain;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
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

/** Line of the consolidated ledger of a run (translated balance, CTA or elimination). */
@Entity
@Table(name = "con_run_line")
public class ConsolidationRunLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "run_id", nullable = false)
  private ConsolidationRun run;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "line_type", nullable = false, length = 20)
  private ConsolidationLineType type;

  @Column(name = "rule_code", length = 30)
  private String ruleCode;

  @Column(name = "company_id")
  private Long companyId;

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  @Column(name = "account_name", nullable = false, length = 150)
  private String accountName;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_class", nullable = false, length = 20)
  private AccountClass accountClass;

  @Column(name = "local_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal localAmount;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal rate;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(length = 250)
  private String description;

  protected ConsolidationRunLine() {}

  ConsolidationRunLine(ConsolidationRun run, int lineNo, ConsolidationLineValues v) {
    this.run = run;
    this.lineNo = lineNo;
    this.type = v.type();
    this.ruleCode = v.ruleCode();
    this.companyId = v.companyId();
    this.accountCode = v.accountCode();
    this.accountName = v.accountName();
    this.accountClass = v.accountClass();
    this.localAmount = v.localAmount();
    this.rate = v.rate();
    this.amount = v.amount();
    this.description = v.description();
  }

  public Long getId() {
    return id;
  }

  public ConsolidationRun getRun() {
    return run;
  }

  public int getLineNo() {
    return lineNo;
  }

  public ConsolidationLineType getType() {
    return type;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public String getAccountName() {
    return accountName;
  }

  public AccountClass getAccountClass() {
    return accountClass;
  }

  public BigDecimal getLocalAmount() {
    return localAmount;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getDescription() {
    return description;
  }
}
