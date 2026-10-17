package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.brokerverse.receivables.domain.PayerType;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * New official receipt.
 *
 * @param companyId company
 * @param branchId collecting branch
 * @param receiptDate receipt date
 * @param payerType payer type
 * @param partyCode payer party (not for OTHER)
 * @param payerName payer name (OTHER receipts)
 * @param department department dimension code
 * @param mode payment instrument (PDC receipts come from the PDC register)
 * @param instrumentNo cheque / transfer / card reference
 * @param instrumentDate cheque date
 * @param draweeBank payer's bank
 * @param currency currency
 * @param amount amount
 * @param bankAccountCode GL bank account receiving the money
 * @param incomeAccountCode income account (OTHER receipts)
 * @param allocationMethod allocation method
 * @param narration narration
 * @param allocations manual allocations
 */
public record ReceiptRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull LocalDate receiptDate,
    @NotNull PayerType payerType,
    @Size(max = 30) String partyCode,
    @Size(max = 200) String payerName,
    @Size(max = 20) String department,
    @NotNull ReceiptMode mode,
    @Size(max = 40) String instrumentNo,
    LocalDate instrumentDate,
    @Size(max = 120) String draweeBank,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @NotBlank @Size(max = 30) String bankAccountCode,
    @Size(max = 30) String incomeAccountCode,
    @NotNull AllocationMethod allocationMethod,
    @Size(max = 250) String narration,
    @Valid List<AllocationRequest> allocations) {

  /** Canonical constructor normalising the allocation list. */
  public ReceiptRequest {
    allocations = allocations == null ? List.of() : List.copyOf(allocations);
  }
}
