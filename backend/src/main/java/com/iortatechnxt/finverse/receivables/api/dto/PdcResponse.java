package com.iortatechnxt.finverse.receivables.api.dto;

import com.iortatechnxt.finverse.receivables.domain.PdcEvent;
import com.iortatechnxt.finverse.receivables.domain.PdcStatus;
import com.iortatechnxt.finverse.receivables.domain.PostDatedCheque;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Post-dated cheque received.
 *
 * @param id id
 * @param pdcNo register number
 * @param branchId branch (division)
 * @param receivedDate received date
 * @param partyCode payer
 * @param payerName payer name
 * @param department department
 * @param chequeNo cheque number
 * @param chequeDate cheque (due) date
 * @param draweeBank payer's bank
 * @param currency currency
 * @param amount amount
 * @param baseAmount amount in base currency
 * @param bankAccountCode bank GL account for the deposit
 * @param debitItemId linked debit note
 * @param narration narration
 * @param status status
 * @param statusDate date of the last status change
 * @param receiptId receipt raised on deposit
 * @param replacedById replacing cheque
 * @param history status history (detail view only)
 */
public record PdcResponse(
    Long id,
    String pdcNo,
    Long branchId,
    LocalDate receivedDate,
    String partyCode,
    String payerName,
    String department,
    String chequeNo,
    LocalDate chequeDate,
    String draweeBank,
    String currency,
    BigDecimal amount,
    BigDecimal baseAmount,
    String bankAccountCode,
    Long debitItemId,
    String narration,
    PdcStatus status,
    LocalDate statusDate,
    Long receiptId,
    Long replacedById,
    List<Event> history) {

  /**
   * Maps a cheque.
   *
   * @param p cheque
   * @param events history (empty for lists)
   * @return response
   */
  public static PdcResponse from(PostDatedCheque p, List<PdcEvent> events) {
    return new PdcResponse(
        p.getId(),
        p.getPdcNo(),
        p.getBranchId(),
        p.getReceivedDate(),
        p.getPartyCode(),
        p.getPayerName(),
        p.getDepartment(),
        p.getChequeNo(),
        p.getChequeDate(),
        p.getDraweeBank(),
        p.getCurrency(),
        p.getAmount(),
        p.getBaseAmount(),
        p.getBankAccountCode(),
        p.getDebitItemId(),
        p.getNarration(),
        p.getStatus(),
        p.getStatusDate(),
        p.getReceiptId(),
        p.getReplacedById(),
        events.stream().map(Event::from).toList());
  }

  /**
   * Status change.
   *
   * @param fromStatus previous status
   * @param toStatus new status
   * @param eventDate date
   * @param remarks remarks
   * @param receiptNo related receipt
   * @param createdBy user
   * @param createdAt time
   */
  public record Event(
      PdcStatus fromStatus,
      PdcStatus toStatus,
      LocalDate eventDate,
      String remarks,
      String receiptNo,
      String createdBy,
      Instant createdAt) {

    static Event from(PdcEvent e) {
      return new Event(
          e.getFromStatus(),
          e.getToStatus(),
          e.getEventDate(),
          e.getRemarks(),
          e.getReceiptNo(),
          e.getCreatedBy(),
          e.getCreatedAt());
    }
  }
}
