package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.payables.domain.PaymentCategory;
import com.iortatechnxt.brokerverse.payables.domain.PaymentMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Values to create or update a payment voucher.
 *
 * @param companyId company
 * @param branchId paying branch
 * @param partyCode payee party (immutable after creation)
 * @param payeeName name on the cheque, null = party name
 * @param category what is paid, null = default for the party and items
 * @param mode payment mode
 * @param bankAccountId paying bank account
 * @param voucherDate payment date
 * @param chequeDate cheque date (post-dated cheques only)
 * @param department department dimension (optional)
 * @param narration narration
 * @param items open items to pay with amounts
 */
public record PaymentCommand(
    Long companyId,
    Long branchId,
    String partyCode,
    String payeeName,
    PaymentCategory category,
    PaymentMode mode,
    Long bankAccountId,
    LocalDate voucherDate,
    LocalDate chequeDate,
    String department,
    String narration,
    List<ItemPayment> items) {

  /** Canonical constructor copying the items. */
  public PaymentCommand {
    items = List.copyOf(items);
  }

  /**
   * Amount paid against one open item.
   *
   * @param openItemId open CREDIT item
   * @param amount amount, null = full available balance
   */
  public record ItemPayment(Long openItemId, BigDecimal amount) {}
}
