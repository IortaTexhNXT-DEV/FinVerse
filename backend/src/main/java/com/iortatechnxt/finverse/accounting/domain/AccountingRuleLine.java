package com.iortatechnxt.finverse.accounting.domain;

import com.iortatechnxt.finverse.coa.domain.BalanceSide;
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

/**
 * One debit or credit instruction of an accounting rule.
 *
 * <p>{@code accountCode} is either a GL account code or an account role placeholder starting with
 * {@value #ROLE_PREFIX} (e.g. {@code @BANK}) resolved from the event at posting time. The line
 * amount is the event's {@code amountComponent}; a negative amount posts to the opposite side.
 */
@Entity
@Table(name = "acc_rule_line")
public class AccountingRuleLine {

  /** Prefix of account role placeholders. */
  public static final String ROLE_PREFIX = "@";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rule_id", nullable = false)
  private AccountingRule rule;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 6)
  private BalanceSide side;

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  @Column(name = "amount_component", nullable = false, length = 40)
  private String amountComponent;

  @Column(name = "party_line", nullable = false)
  private boolean partyLine;

  @Column(length = 200)
  private String narration;

  protected AccountingRuleLine() {}

  /**
   * Creates a rule line.
   *
   * @param side debit or credit
   * @param accountCode GL account code or {@code @ROLE}
   * @param amountComponent event amount component
   * @param partyLine whether the event party is carried on the line (control accounts)
   * @param narration optional narration
   */
  public AccountingRuleLine(
      BalanceSide side,
      String accountCode,
      String amountComponent,
      boolean partyLine,
      String narration) {
    this.side = side;
    this.accountCode = accountCode;
    this.amountComponent = amountComponent;
    this.partyLine = partyLine;
    this.narration = narration;
  }

  void attach(AccountingRule owner, int number) {
    this.rule = owner;
    this.lineNo = number;
  }

  /**
   * Whether the account is a role placeholder.
   *
   * @return true for {@code @ROLE} accounts
   */
  public boolean isAccountRole() {
    return accountCode.startsWith(ROLE_PREFIX);
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public BalanceSide getSide() {
    return side;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public String getAmountComponent() {
    return amountComponent;
  }

  public boolean isPartyLine() {
    return partyLine;
  }

  public String getNarration() {
    return narration;
  }
}
