package com.iortatechnxt.finverse.payables.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reimbursement (replenishment) claim of a petty cash fund: groups approved disbursement vouchers;
 * once approved the bank refills the box by their total (Dr petty cash / Cr bank).
 */
@Entity
@Table(name = "pay_petty_cash_reimbursement")
public class PettyCashReimbursement extends PettyCashDocument {

  @Column(name = "claim_date", nullable = false)
  private LocalDate claimDate;

  @Column(name = "bank_account_id", nullable = false)
  private Long bankAccountId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(length = 250)
  private String narration;

  protected PettyCashReimbursement() {}

  /**
   * Creates a claim (pending approval).
   *
   * @param fund fund
   * @param documentNo document number
   * @param claimDate claim date
   * @param bankAccountId bank account paying the replenishment
   * @param amount total of the claimed vouchers
   * @param narration narration
   */
  public PettyCashReimbursement(
      PettyCashFund fund,
      String documentNo,
      LocalDate claimDate,
      Long bankAccountId,
      BigDecimal amount,
      String narration) {
    super(fund, documentNo);
    this.claimDate = claimDate;
    this.bankAccountId = bankAccountId;
    this.amount = amount;
    this.narration = narration;
  }

  public LocalDate getClaimDate() {
    return claimDate;
  }

  public Long getBankAccountId() {
    return bankAccountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getNarration() {
    return narration;
  }
}
