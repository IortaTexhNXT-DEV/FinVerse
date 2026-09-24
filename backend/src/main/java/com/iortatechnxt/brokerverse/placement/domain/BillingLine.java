package com.iortatechnxt.brokerverse.placement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The CLPC billing fields of one account (BRNB.067).
 *
 * @param accountId account
 * @param arn reference (Account Reference Number)
 * @param pnNumbers promissory note numbers
 * @param loanApplicationNo loan application number
 * @param bookingDate booking date (the account's period start until the CLPC layout is confirmed,
 *     Q28)
 * @param borrower borrower (client name)
 * @param originatingUnit originating unit (sales team)
 * @param premium gross premium
 * @param bdoiLocation BDOI location (sales region)
 * @param amortised premium amortised over a multi-year term
 */
public record BillingLine(
    Long accountId,
    String arn,
    List<String> pnNumbers,
    String loanApplicationNo,
    LocalDate bookingDate,
    String borrower,
    String originatingUnit,
    BigDecimal premium,
    String bdoiLocation,
    boolean amortised) {

  /** Defensive copy. */
  public BillingLine {
    pnNumbers = pnNumbers == null ? List.of() : List.copyOf(pnNumbers);
  }
}
