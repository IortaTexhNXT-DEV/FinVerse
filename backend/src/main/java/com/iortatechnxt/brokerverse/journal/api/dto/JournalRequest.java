package com.iortatechnxt.brokerverse.journal.api.dto;

import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Manual journal create / update request.
 *
 * @param companyId company
 * @param branchId originating branch
 * @param journalType MANUAL, ADJUSTMENT or ACCRUAL
 * @param valueDate accounting date
 * @param currency header currency
 * @param narration narration
 * @param reference reference
 * @param lines lines (at least two for submission; drafts may be incomplete)
 * @param reverseOn date on which the posted journal is reversed automatically (FRBS 2.8.1), null
 *     for none
 */
public record JournalRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull JournalType journalType,
    @NotNull LocalDate valueDate,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotBlank @Size(max = 500) String narration,
    @Size(max = 60) String reference,
    @NotEmpty @Size(max = 500) List<@Valid JournalLineRequest> lines,
    LocalDate reverseOn) {

  /**
   * Request without an automatic reversal date.
   *
   * @param companyId company
   * @param branchId branch
   * @param journalType type
   * @param valueDate accounting date
   * @param currency header currency
   * @param narration narration
   * @param reference reference
   * @param lines lines
   */
  public JournalRequest(
      Long companyId,
      Long branchId,
      JournalType journalType,
      LocalDate valueDate,
      String currency,
      String narration,
      String reference,
      List<JournalLineRequest> lines) {
    this(companyId, branchId, journalType, valueDate, currency, narration, reference, lines, null);
  }
}
