package com.iortatechnxt.brokerverse.consolidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Subsidiary of a consolidation group with ownership and investment elimination settings. */
@Entity
@Table(name = "con_group_member")
public class ConsolidationMember {

  private static final String SEPARATOR = ",";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private ConsolidationGroup group;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "ownership_pct", nullable = false, precision = 7, scale = 4)
  private BigDecimal ownershipPct;

  @Column(name = "investment_account", length = 30)
  private String investmentAccount;

  @Column(name = "equity_accounts", length = 200)
  private String equityAccounts;

  protected ConsolidationMember() {}

  ConsolidationMember(ConsolidationGroup group, MemberValues values) {
    this.group = group;
    this.companyId = values.companyId();
    apply(values);
  }

  /**
   * Updates ownership and elimination settings.
   *
   * @param values new values (same company)
   */
  void update(MemberValues values) {
    apply(values);
  }

  private void apply(MemberValues values) {
    this.ownershipPct = values.ownershipPct();
    this.investmentAccount = values.investmentAccount();
    this.equityAccounts =
        values.equityAccounts().isEmpty() ? null : String.join(SEPARATOR, values.equityAccounts());
  }

  /**
   * Share capital accounts of the subsidiary.
   *
   * @return account codes
   */
  public List<String> equityAccountCodes() {
    if (equityAccounts == null || equityAccounts.isBlank()) {
      return List.of();
    }
    return Arrays.stream(equityAccounts.split(SEPARATOR))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  public Long getId() {
    return id;
  }

  public ConsolidationGroup getGroup() {
    return group;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public BigDecimal getOwnershipPct() {
    return ownershipPct;
  }

  public String getInvestmentAccount() {
    return investmentAccount;
  }
}
