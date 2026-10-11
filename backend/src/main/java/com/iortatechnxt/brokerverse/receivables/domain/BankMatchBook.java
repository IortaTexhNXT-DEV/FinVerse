package com.iortatechnxt.brokerverse.receivables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Ledger entry reconciled by a {@link BankMatch}. */
@Entity
@Table(name = "brs_match_book")
public class BankMatchBook extends BaseEntity {

  @Column(name = "match_id", nullable = false)
  private Long matchId;

  @Column(name = "ledger_entry_id", nullable = false)
  private Long ledgerEntryId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  protected BankMatchBook() {}

  /**
   * Creates the link.
   *
   * @param matchId match
   * @param ledgerEntryId ledger entry
   * @param amount signed base amount of the entry (debit positive)
   */
  public BankMatchBook(Long matchId, Long ledgerEntryId, BigDecimal amount) {
    this.matchId = matchId;
    this.ledgerEntryId = ledgerEntryId;
    this.amount = amount;
  }

  public Long getMatchId() {
    return matchId;
  }

  public Long getLedgerEntryId() {
    return ledgerEntryId;
  }

  public BigDecimal getAmount() {
    return amount;
  }
}
