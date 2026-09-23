package com.iortatechnxt.finverse.journal.api.dto;

import com.iortatechnxt.finverse.journal.domain.JournalSearchCriteria;
import com.iortatechnxt.finverse.journal.domain.JournalStatus;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the journal inquiry, bound from the request URL.
 *
 * @param companyId company (mandatory)
 * @param branchId branch
 * @param status status
 * @param journalType type
 * @param fromDate value date from
 * @param toDate value date to
 * @param batchNo batch number prefix
 * @param inputter maker
 * @param authorizer checker
 * @param minAmount cut-off amount
 * @param sourceModule source module
 */
public record JournalSearchParams(
    @NotNull Long companyId,
    Long branchId,
    JournalStatus status,
    JournalType journalType,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
    String batchNo,
    String inputter,
    String authorizer,
    BigDecimal minAmount,
    String sourceModule) {

  /**
   * Converts to domain search criteria.
   *
   * @return criteria
   */
  public JournalSearchCriteria toCriteria() {
    return new JournalSearchCriteria(
        companyId,
        branchId,
        status,
        journalType,
        fromDate,
        toDate,
        batchNo,
        inputter,
        authorizer,
        minAmount,
        sourceModule);
  }
}
