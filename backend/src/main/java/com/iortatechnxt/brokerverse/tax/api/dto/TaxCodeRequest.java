package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.tax.domain.PayeeClass;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import com.iortatechnxt.brokerverse.tax.service.TaxCodeCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create / update tax code request ({@code code} is immutable after creation).
 *
 * @param companyId company
 * @param code code (the ATC for withholding codes)
 * @param name name
 * @param taxType tax type
 * @param atc alphanumeric tax code (EWT)
 * @param payeeClass payee class (EWT)
 * @param rate rate in percent
 * @param glAccountCode GL account
 * @param incomeNature nature of income (EWT)
 * @param effectiveFrom first day of validity
 * @param effectiveTo last day of validity (optional)
 */
public record TaxCodeRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9_\\-]+") String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull TaxType taxType,
    @Size(max = 10) @Pattern(regexp = "[A-Z0-9]*") String atc,
    PayeeClass payeeClass,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal rate,
    @NotBlank @Size(max = 30) String glAccountCode,
    @Size(max = 200) String incomeNature,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo) {

  /**
   * Converts to the service command.
   *
   * @return command
   */
  public TaxCodeCommand toCommand() {
    return new TaxCodeCommand(
        companyId,
        code,
        name,
        taxType,
        atc,
        payeeClass,
        rate,
        glAccountCode,
        incomeNature,
        effectiveFrom,
        effectiveTo);
  }
}
