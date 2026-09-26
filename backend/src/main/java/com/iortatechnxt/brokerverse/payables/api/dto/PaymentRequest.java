package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.domain.PaymentCategory;
import com.iortatechnxt.brokerverse.payables.domain.PaymentMode;
import com.iortatechnxt.brokerverse.payables.service.PaymentCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create / update payment voucher request.
 *
 * @param companyId company
 * @param branchId paying branch
 * @param partyCode payee
 * @param payeeName name on the cheque (optional)
 * @param category what is paid (optional: derived)
 * @param mode payment mode
 * @param bankAccountId paying bank account
 * @param voucherDate payment date
 * @param chequeDate cheque date (PDC only)
 * @param department department (optional)
 * @param narration narration
 * @param items open items paid
 */
public record PaymentRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotBlank @Size(max = 30) String partyCode,
    @Size(max = 200) String payeeName,
    PaymentCategory category,
    @NotNull PaymentMode mode,
    @NotNull Long bankAccountId,
    @NotNull LocalDate voucherDate,
    LocalDate chequeDate,
    @Size(max = 20) String department,
    @Size(max = 200) String narration,
    @NotEmpty @Size(max = 200) List<@Valid Item> items) {

  /** Canonical constructor copying the items. */
  public PaymentRequest {
    items = items == null ? List.of() : List.copyOf(items);
  }

  /**
   * Converts to the service command.
   *
   * @return command
   */
  public PaymentCommand toCommand() {
    return new PaymentCommand(
        companyId,
        branchId,
        partyCode,
        payeeName,
        category,
        mode,
        bankAccountId,
        voucherDate,
        chequeDate,
        department,
        narration,
        items.stream()
            .map(i -> new PaymentCommand.ItemPayment(i.openItemId(), i.amount()))
            .toList());
  }

  /**
   * Item paid.
   *
   * @param openItemId open CREDIT item
   * @param amount amount (optional: full available balance)
   */
  public record Item(
      @NotNull Long openItemId,
      @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {}
}
