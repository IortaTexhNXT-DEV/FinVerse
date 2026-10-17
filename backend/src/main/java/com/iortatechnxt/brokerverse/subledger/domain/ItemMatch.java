package com.iortatechnxt.brokerverse.subledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Settlement of a DEBIT open item by a CREDIT open item (knock-off / allocation). */
@Entity
@Table(name = "sl_item_match")
public class ItemMatch extends BaseEntity {

  @Column(name = "debit_item_id", nullable = false)
  private Long debitItemId;

  @Column(name = "credit_item_id", nullable = false)
  private Long creditItemId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "match_date", nullable = false)
  private LocalDate matchDate;

  protected ItemMatch() {}

  /**
   * Creates a match.
   *
   * @param debitItemId debit item
   * @param creditItemId credit item
   * @param amount matched amount (item currency)
   * @param matchDate date of matching
   */
  public ItemMatch(Long debitItemId, Long creditItemId, BigDecimal amount, LocalDate matchDate) {
    this.debitItemId = debitItemId;
    this.creditItemId = creditItemId;
    this.amount = amount;
    this.matchDate = matchDate;
  }

  public Long getDebitItemId() {
    return debitItemId;
  }

  public Long getCreditItemId() {
    return creditItemId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getMatchDate() {
    return matchDate;
  }
}
