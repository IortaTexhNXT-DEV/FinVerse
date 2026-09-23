package com.iortatechnxt.finverse.claims.domain;

import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Salvage or subrogation money received into a bank account, at 100 %. On approval it consumes the
 * recovery estimate and reduces claims expense by the company share.
 */
@Entity
@Table(name = "clm_recovery")
public class Recovery extends ClaimDocument {

  @Column(name = "recovery_no", nullable = false, length = 40)
  private String recoveryNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "recovery_type", nullable = false, length = 15)
  private RecoveryType recoveryType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_party_id")
  private Party fromParty;

  @Column(name = "bank_account_code", nullable = false, length = 20)
  private String bankAccountCode;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "our_amount", precision = 19, scale = 2)
  private BigDecimal ourAmount;

  @Column(name = "coinsurer_amount", precision = 19, scale = 2)
  private BigDecimal coinsurerAmount;

  @Column(nullable = false, length = 300)
  private String narration;

  @Column(name = "coinsurance_batch_no", length = 40)
  private String coinsuranceBatchNo;

  protected Recovery() {}

  /**
   * Creates a recovery pending approval.
   *
   * @param claim claim
   * @param recoveryNo allocated number
   * @param terms type, payer, bank account, amount and narration
   * @param approval maker-checker state
   */
  public Recovery(Claim claim, String recoveryNo, RecoveryTerms terms, ClaimApproval approval) {
    super(claim, approval);
    this.recoveryNo = recoveryNo;
    this.recoveryType = terms.recoveryType();
    this.fromParty = terms.fromParty();
    this.bankAccountCode = terms.bankAccountCode();
    this.amount = terms.amount();
    this.narration = terms.narration();
  }

  /**
   * Records the accounting split made at approval.
   *
   * @param split company share and coinsurers' share
   * @param coinsuranceBatch journal of the coinsurers' share, null when not leading
   */
  public void recordSplit(ShareSplit split, String coinsuranceBatch) {
    this.ourAmount = split.ours();
    this.coinsurerAmount = split.coinsurers();
    this.coinsuranceBatchNo = coinsuranceBatch;
  }

  @Override
  public String label() {
    return "Recovery " + recoveryNo;
  }

  public String getRecoveryNo() {
    return recoveryNo;
  }

  public RecoveryType getRecoveryType() {
    return recoveryType;
  }

  public Party getFromParty() {
    return fromParty;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getOurAmount() {
    return ourAmount;
  }

  public BigDecimal getCoinsurerAmount() {
    return coinsurerAmount;
  }

  public String getNarration() {
    return narration;
  }

  public String getCoinsuranceBatchNo() {
    return coinsuranceBatchNo;
  }
}
