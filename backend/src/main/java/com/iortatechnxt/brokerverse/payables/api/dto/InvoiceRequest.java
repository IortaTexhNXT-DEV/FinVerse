package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.domain.InvoiceLineValues;
import com.iortatechnxt.brokerverse.payables.service.InvoiceCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create / update supplier invoice request.
 *
 * @param companyId company
 * @param branchId branch
 * @param partyCode supplier
 * @param supplierInvoiceNo supplier's invoice number
 * @param invoiceDate invoice date
 * @param dueDate due date (optional: supplier credit days)
 * @param currency currency (optional: supplier default)
 * @param vatApplicable 12 % input VAT applies
 * @param narration narration
 * @param lines expense lines
 */
public record InvoiceRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotBlank @Size(max = 30) String partyCode,
    @NotBlank @Size(max = 40) String supplierInvoiceNo,
    @NotNull LocalDate invoiceDate,
    LocalDate dueDate,
    @Pattern(regexp = "[A-Z]{3}") String currency,
    boolean vatApplicable,
    @Size(max = 200) String narration,
    @NotEmpty @Size(max = 100) List<@Valid Line> lines) {

  /** Canonical constructor copying the lines. */
  public InvoiceRequest {
    lines = lines == null ? List.of() : List.copyOf(lines);
  }

  /**
   * Converts to the service command.
   *
   * @return command
   */
  public InvoiceCommand toCommand() {
    return new InvoiceCommand(
        companyId,
        branchId,
        partyCode,
        supplierInvoiceNo,
        invoiceDate,
        dueDate,
        currency,
        vatApplicable,
        narration,
        lines.stream()
            .map(
                l ->
                    new InvoiceLineValues(
                        l.expenseAccountCode(), l.costCenter(), l.description(), l.netAmount()))
            .toList());
  }

  /**
   * Expense line.
   *
   * @param expenseAccountCode GL account
   * @param costCenter cost centre
   * @param description description
   * @param netAmount amount net of VAT
   */
  public record Line(
      @NotBlank @Size(max = 30) String expenseAccountCode,
      @Size(max = 20) String costCenter,
      @NotBlank @Size(max = 200) String description,
      @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal netAmount) {}
}
