package com.iortatechnxt.brokerverse.cashiering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * The maintained minimal-balance table (CSHID.016, Cashiering summary 5.f, OQ11): the maximum
 * amount per kind of balance, the exclusions of CSHID.016 and the action.
 */
@Entity
@Table(name = "csh_minimal_balance_rule")
public class MinimalBalanceRule {

  /** Premium receivable balances. */
  public static final String PREMIUM = "PREMIUM";

  /** Unapplied and excess payments. */
  public static final String EXCESS = "EXCESS";

  @Id
  @Column(length = 20)
  private String kind;

  @Column(name = "max_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal maxAmount;

  @Column(name = "exclude_cwt", nullable = false)
  private boolean excludeCwt;

  @Column(name = "exclude_dst", nullable = false)
  private boolean excludeDst;

  @Column(name = "exclude_whole_premium", nullable = false)
  private boolean excludeWholePremium;

  @Column(nullable = false, length = 20)
  private String action;

  @Column(nullable = false)
  private boolean active;

  @Column(nullable = false, length = 250)
  private String description;

  protected MinimalBalanceRule() {}

  public String getKind() {
    return kind;
  }

  public BigDecimal getMaxAmount() {
    return maxAmount;
  }

  public boolean isExcludeCwt() {
    return excludeCwt;
  }

  public boolean isExcludeDst() {
    return excludeDst;
  }

  public boolean isExcludeWholePremium() {
    return excludeWholePremium;
  }

  public String getAction() {
    return action;
  }

  public boolean isActive() {
    return active;
  }

  public String getDescription() {
    return description;
  }
}
