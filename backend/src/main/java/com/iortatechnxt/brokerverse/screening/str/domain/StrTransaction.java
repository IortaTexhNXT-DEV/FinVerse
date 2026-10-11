package com.iortatechnxt.brokerverse.screening.str.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A transaction reported on an STR (SNSRP-705; FR-SS-070): reference (account, invoice, receipt or
 * policy), date, amount (greater than 0), currency, type and description; prefilled from the
 * client's records and editable.
 */
@Entity
@Table(name = "scr_str_transaction")
public class StrTransaction extends BaseEntity {

  @Column(name = "str_id", nullable = false, updatable = false)
  private Long strId;

  @Column(name = "reference", nullable = false, length = 60, updatable = false)
  private String reference;

  @Column(name = "txn_date", nullable = false, updatable = false)
  private LocalDate txnDate;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "txn_type", nullable = false, length = 30, updatable = false)
  private String txnType;

  @Column(name = "description", length = 500, updatable = false)
  private String description;

  /** For JPA. */
  protected StrTransaction() {}

  /**
   * Records a transaction.
   *
   * @param strId the STR
   * @param line reference, date, amount, currency, type and description
   */
  public StrTransaction(Long strId, Line line) {
    this.strId = strId;
    this.reference = line.reference();
    this.txnDate = line.date();
    this.amount = line.amount();
    this.currency = line.currency();
    this.txnType = line.type();
    this.description = line.description();
  }

  /**
   * The transaction as a line.
   *
   * @return line
   */
  public Line line() {
    return new Line(reference, txnDate, amount, currency, txnType, description);
  }

  public Long getStrId() {
    return strId;
  }

  /**
   * One transaction line.
   *
   * @param reference account, invoice, receipt or policy reference
   * @param date transaction date
   * @param amount amount (greater than 0, scale 2)
   * @param currency ISO currency
   * @param type ACCOUNT, INVOICE, RECEIPT, POLICY or OTHER
   * @param description description, may be null
   */
  public record Line(
      String reference,
      LocalDate date,
      BigDecimal amount,
      String currency,
      String type,
      String description) {}
}
