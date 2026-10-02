package com.iortatechnxt.brokerverse.consolidation.api.dto;

import com.iortatechnxt.brokerverse.consolidation.domain.IntercompanyTransaction;
import com.iortatechnxt.brokerverse.consolidation.domain.IntercompanyTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Inter-company transaction view.
 *
 * @param id id
 * @param icReference shared reference
 * @param type CHARGE or SETTLEMENT
 * @param creditorCompanyId creditor company
 * @param debtorCompanyId debtor company
 * @param valueDate value date
 * @param currency currency
 * @param amount amount
 * @param creditorAccount creditor counter account
 * @param debtorAccount debtor counter account
 * @param narration narration
 * @param creditorBatchNo creditor journal
 * @param debtorBatchNo debtor journal
 * @param createdBy user
 * @param createdAt time
 */
public record IntercompanyTransactionResponse(
    Long id,
    String icReference,
    IntercompanyTransactionType type,
    Long creditorCompanyId,
    Long debtorCompanyId,
    LocalDate valueDate,
    String currency,
    BigDecimal amount,
    String creditorAccount,
    String debtorAccount,
    String narration,
    String creditorBatchNo,
    String debtorBatchNo,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps an entity.
   *
   * @param t transaction
   * @return view
   */
  public static IntercompanyTransactionResponse from(IntercompanyTransaction t) {
    return new IntercompanyTransactionResponse(
        t.getId(),
        t.getIcReference(),
        t.getType(),
        t.getCreditorCompanyId(),
        t.getDebtorCompanyId(),
        t.getValueDate(),
        t.getCurrency(),
        t.getAmount(),
        t.getCreditorAccount(),
        t.getDebtorAccount(),
        t.getNarration(),
        t.getCreditorBatchNo(),
        t.getDebtorBatchNo(),
        t.getCreatedBy(),
        t.getCreatedAt());
  }
}
