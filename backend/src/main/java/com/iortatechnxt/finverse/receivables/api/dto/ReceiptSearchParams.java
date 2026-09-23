package com.iortatechnxt.finverse.receivables.api.dto;

import com.iortatechnxt.finverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.finverse.receivables.domain.ReceiptSearch;
import com.iortatechnxt.finverse.receivables.domain.ReceiptStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Receipt search query parameters.
 *
 * @param companyId company
 * @param status status
 * @param partyCode party
 * @param mode instrument
 * @param from receipt date from
 * @param to receipt date to
 * @param receiptNo receipt number (contains)
 */
public record ReceiptSearchParams(
    @NotNull Long companyId,
    ReceiptStatus status,
    String partyCode,
    ReceiptMode mode,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    String receiptNo) {

  /**
   * Converts to domain criteria.
   *
   * @return criteria
   */
  public ReceiptSearch toSearch() {
    return new ReceiptSearch(companyId, status, partyCode, mode, from, to, receiptNo);
  }
}
