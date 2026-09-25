package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import java.time.LocalDate;

/**
 * Criteria of an Operations invoice search (RMTID.026, ADJID.024); null criteria are ignored.
 *
 * @param companyId company (mandatory)
 * @param text part of the invoice no., ARN, policy no., client code or assured name
 * @param insurerCode lead insurer
 * @param clientCode client
 * @param paymentStatus payment status
 * @param remittanceStatus remittance status
 * @param flag flag that must be set
 * @param locked true for locked invoices only, false for unlocked only
 * @param directPayment true for direct payment invoices only, false to exclude them
 * @param from booked on or after
 * @param to booked on or before
 * @param assured part of the assured name (DIS 3.27.2 invoice search)
 * @param inceptionFrom period starting on or after
 * @param inceptionTo period starting on or before
 * @param aoUsername account officer
 */
public record LedgerSearch(
    Long companyId,
    String text,
    String insurerCode,
    String clientCode,
    PaymentStatus paymentStatus,
    RemittanceStatus remittanceStatus,
    InvoiceFlag flag,
    Boolean locked,
    Boolean directPayment,
    LocalDate from,
    LocalDate to,
    String assured,
    LocalDate inceptionFrom,
    LocalDate inceptionTo,
    String aoUsername) {

  /**
   * Criteria without the assured, inception and account officer filters (earlier contract).
   *
   * @param companyId company (mandatory)
   * @param text part of the invoice no., ARN, policy no., client code or assured name
   * @param insurerCode lead insurer
   * @param clientCode client
   * @param paymentStatus payment status
   * @param remittanceStatus remittance status
   * @param flag flag that must be set
   * @param locked true for locked invoices only, false for unlocked only
   * @param directPayment true for direct payment invoices only, false to exclude them
   * @param from booked on or after
   * @param to booked on or before
   */
  @SuppressWarnings("java:S107") // search criteria
  public LedgerSearch(
      Long companyId,
      String text,
      String insurerCode,
      String clientCode,
      PaymentStatus paymentStatus,
      RemittanceStatus remittanceStatus,
      InvoiceFlag flag,
      Boolean locked,
      Boolean directPayment,
      LocalDate from,
      LocalDate to) {
    this(
        companyId,
        text,
        insurerCode,
        clientCode,
        paymentStatus,
        remittanceStatus,
        flag,
        locked,
        directPayment,
        from,
        to,
        null,
        null,
        null,
        null);
  }

  /**
   * Every invoice of a company.
   *
   * @param companyId company
   * @return criteria
   */
  public static LedgerSearch all(Long companyId) {
    return new LedgerSearch(companyId, null, null, null, null, null, null, null, null, null, null);
  }
}
