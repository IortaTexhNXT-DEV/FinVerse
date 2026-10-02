package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.tax.domain.FilingFrequency;
import com.iortatechnxt.brokerverse.tax.domain.TaxAuthority;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.TaxFormCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Create / update tax form request ({@code code} is immutable after creation).
 *
 * @param companyId company
 * @param code form code
 * @param name name
 * @param authority authority
 * @param frequency filing frequency
 * @param worksheet computing worksheet
 * @param dueMonthsAfter months after the period-end month
 * @param dueDay due day (31 = month end)
 * @param payableAccountCode tax payable account (tracked forms)
 * @param creditAccountCode credit account (optional)
 * @param trackFiling returns and alerts managed in BrokerVerse
 * @param effectiveFrom first period tracked
 */
public record TaxFormRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9\\-]+") String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull TaxAuthority authority,
    @NotNull FilingFrequency frequency,
    @NotNull WorksheetKind worksheet,
    @Min(0) @Max(12) int dueMonthsAfter,
    @Min(1) @Max(31) int dueDay,
    @Size(max = 30) String payableAccountCode,
    @Size(max = 30) String creditAccountCode,
    boolean trackFiling,
    @NotNull LocalDate effectiveFrom) {

  /**
   * Converts to the service command.
   *
   * @return command
   */
  public TaxFormCommand toCommand() {
    return new TaxFormCommand(
        companyId,
        code,
        name,
        authority,
        frequency,
        worksheet,
        dueMonthsAfter,
        dueDay,
        payableAccountCode,
        creditAccountCode,
        trackFiling,
        effectiveFrom);
  }
}
