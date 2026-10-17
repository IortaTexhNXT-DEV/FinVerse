package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.CreditRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Credit of a service invoice.
 *
 * @param commission commission to credit, null for all that is left
 * @param vatOnCommission VAT to credit, null for all that is left
 * @param reason reason
 */
public record ServiceInvoiceCreditRequest(
    @DecimalMin("0") BigDecimal commission,
    @DecimalMin("0") BigDecimal vatOnCommission,
    @NotBlank @Size(max = 500) String reason) {

  /**
   * The credit request.
   *
   * @return request
   */
  public CreditRequest toRequest() {
    return new CreditRequest(commission, vatOnCommission, null, reason);
  }
}
