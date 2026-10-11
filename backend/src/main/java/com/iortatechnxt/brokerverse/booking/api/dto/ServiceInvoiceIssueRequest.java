package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.IssueRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A manual service invoice (BRNB.100 types with a MANUAL trigger, or any active type).
 *
 * @param companyId company
 * @param typeCode type
 * @param invoiceNo booked invoice, may be blank
 * @param arn account, may be blank
 * @param recipientCode insurer party code or internal unit
 * @param recipientName internal recipient name
 * @param issueDate issue date, null for today
 * @param currency currency
 * @param commission commission line
 * @param vatOnCommission VAT line
 * @param wtaxAmount withholding tax line
 * @param remarks remarks
 */
public record ServiceInvoiceIssueRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 40) String typeCode,
    @Size(max = 40) String invoiceNo,
    @Size(max = 30) String arn,
    @NotBlank @Size(max = 30) String recipientCode,
    @Size(max = 250) String recipientName,
    LocalDate issueDate,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull @DecimalMin("0") BigDecimal commission,
    @NotNull @DecimalMin("0") BigDecimal vatOnCommission,
    @NotNull @DecimalMin("0") BigDecimal wtaxAmount,
    @Size(max = 500) String remarks) {

  /**
   * The issue request.
   *
   * @return request
   */
  public IssueRequest toRequest() {
    return new IssueRequest(
        companyId,
        typeCode,
        blankToNull(invoiceNo),
        blankToNull(arn),
        recipientCode,
        blankToNull(recipientName),
        issueDate,
        currency,
        commission,
        vatOnCommission,
        wtaxAmount,
        remarks);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
