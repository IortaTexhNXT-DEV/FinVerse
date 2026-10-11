package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.TenderType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The money of a creation record: payment type, currency, bank account to post to, paid amount
 * (with VAT and withholding tax for an OR), check details, receipt issuance date, remarks and the
 * SOA or fee invoice of an other income payment (FRS.CSH.02.01.02, 02.02.02, 02.02.08).
 *
 * @param tenderType cash, check or direct credit
 * @param currency PHP or USD
 * @param bankAccount code of the Post to Bank Account list
 * @param amount paid amount
 * @param vat VAT of an OR, may be null
 * @param wtax withholding tax of an OR, may be null
 * @param certificateRef BIR 2307 certificate reference of an OR, may be null
 * @param check check number, date and bank, may be null for cash
 * @param receiptDate receipt issuance date
 * @param remarks remarks (200 characters)
 * @param otherIncomeRef SOA or fee policy invoice settled by an OR, may be null
 */
@Embeddable
public record RecordTender(
    @Enumerated(EnumType.STRING) @Column(name = "tender_type", length = 20) TenderType tenderType,
    @Column(length = 3) String currency,
    @Column(name = "bank_account", length = 40) String bankAccount,
    @Column(precision = 19, scale = 2) BigDecimal amount,
    @Column(precision = 19, scale = 2) BigDecimal vat,
    @Column(precision = 19, scale = 2) BigDecimal wtax,
    @Column(name = "certificate_ref", length = 60) String certificateRef,
    RecordCheck check,
    @Column(name = "receipt_date") LocalDate receiptDate,
    @Column(length = 250) String remarks,
    @Column(name = "other_income_ref", length = 60) String otherIncomeRef) {

  /**
   * Check details of a check payment.
   *
   * @param checkNo check number (40 characters)
   * @param checkDate check date
   * @param checkBank bank of the check
   */
  @Embeddable
  public record RecordCheck(
      @Column(name = "check_no", length = 40) String checkNo,
      @Column(name = "check_date") LocalDate checkDate,
      @Column(name = "check_bank", length = 60) String checkBank) {}

  /**
   * Whether the payment is a check.
   *
   * @return true for a check
   */
  public boolean isCheck() {
    return tenderType == TenderType.CHECK;
  }

  /**
   * The check, never null.
   *
   * @return check details (all null for cash)
   */
  public RecordCheck checkOrEmpty() {
    return check == null ? new RecordCheck(null, null, null) : check;
  }
}
