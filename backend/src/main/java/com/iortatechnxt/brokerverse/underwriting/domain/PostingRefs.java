package com.iortatechnxt.brokerverse.underwriting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * Accounting references of an approved policy or endorsement: client debit (or credit) note,
 * intermediary credit (or debit) note, the GL batches that recorded them and the exchange rate
 * used.
 */
@Embeddable
public class PostingRefs {

  @Column(name = "debit_note_no", length = 40)
  private String debitNoteNo;

  @Column(name = "credit_note_no", length = 40)
  private String creditNoteNo;

  @Column(name = "premium_batch_no", length = 40)
  private String premiumBatchNo;

  @Column(name = "commission_batch_no", length = 40)
  private String commissionBatchNo;

  @Column(name = "exchange_rate", precision = 19, scale = 8)
  private BigDecimal exchangeRate;

  /** Creates empty references (JPA and new documents). */
  public PostingRefs() {
    // filled on approval
  }

  /**
   * Creates references.
   *
   * @param debitNoteNo client document number
   * @param creditNoteNo intermediary document number
   * @param premiumBatchNo premium journal
   * @param commissionBatchNo commission journal
   * @param exchangeRate rate to base currency
   */
  public PostingRefs(
      String debitNoteNo,
      String creditNoteNo,
      String premiumBatchNo,
      String commissionBatchNo,
      BigDecimal exchangeRate) {
    this.debitNoteNo = debitNoteNo;
    this.creditNoteNo = creditNoteNo;
    this.premiumBatchNo = premiumBatchNo;
    this.commissionBatchNo = commissionBatchNo;
    this.exchangeRate = exchangeRate;
  }

  public String getDebitNoteNo() {
    return debitNoteNo;
  }

  public String getCreditNoteNo() {
    return creditNoteNo;
  }

  public String getPremiumBatchNo() {
    return premiumBatchNo;
  }

  public String getCommissionBatchNo() {
    return commissionBatchNo;
  }

  public BigDecimal getExchangeRate() {
    return exchangeRate;
  }
}
