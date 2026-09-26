package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the Operations invoice search.
 *
 * @param q invoice no., ARN, policy no., client code or assured name (part)
 * @param insurer lead insurer
 * @param client client code
 * @param payment payment status
 * @param remittance remittance status
 * @param flag flag set
 * @param locked locked only (true) or unlocked only (false)
 * @param dp direct payment only (true) or excluded (false)
 * @param from booked on or after
 * @param to booked on or before
 * @param assured part of the assured name
 * @param inceptionFrom period starting on or after
 * @param inceptionTo period starting on or before
 * @param ao account officer
 */
public record LedgerSearchParams(
    String q,
    String insurer,
    String client,
    PaymentStatus payment,
    RemittanceStatus remittance,
    InvoiceFlag flag,
    Boolean locked,
    Boolean dp,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    String assured,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inceptionFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inceptionTo,
    String ao) {

  /**
   * The search criteria of a company.
   *
   * @param companyId company
   * @return criteria
   */
  public LedgerSearch toSearch(Long companyId) {
    return new LedgerSearch(
        companyId,
        q,
        insurer,
        client,
        payment,
        remittance,
        flag,
        locked,
        dp,
        from,
        to,
        assured,
        inceptionFrom,
        inceptionTo,
        ao);
  }
}
