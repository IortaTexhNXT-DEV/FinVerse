package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.domain.IssuedPdc;
import com.iortatechnxt.brokerverse.payables.domain.IssuedPdcStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Post-dated cheque issued (register entry).
 *
 * @param id id
 * @param voucherId payment voucher
 * @param bankAccountId bank account
 * @param branchId branch
 * @param partyCode payee
 * @param payeeName payee name
 * @param chequeNo cheque number
 * @param chequeDate cheque date
 * @param issueDate issue date
 * @param currency currency
 * @param amount amount
 * @param baseAmount base currency amount
 * @param department department
 * @param status status
 * @param presentedOn presentation date
 * @param presentationBatchNo presentation journal
 * @param clearedOn clearing date
 * @param cancelledOn cancellation date
 * @param replacedOn replacement date
 * @param replacesId cheque replaced by this one
 * @param replacedById replacement cheque
 * @param remarks remarks
 */
public record IssuedPdcResponse(
    Long id,
    Long voucherId,
    Long bankAccountId,
    Long branchId,
    String partyCode,
    String payeeName,
    String chequeNo,
    LocalDate chequeDate,
    LocalDate issueDate,
    String currency,
    BigDecimal amount,
    BigDecimal baseAmount,
    String department,
    IssuedPdcStatus status,
    LocalDate presentedOn,
    String presentationBatchNo,
    LocalDate clearedOn,
    LocalDate cancelledOn,
    LocalDate replacedOn,
    Long replacesId,
    Long replacedById,
    String remarks) {

  /**
   * Maps an entity.
   *
   * @param p cheque
   * @return response
   */
  public static IssuedPdcResponse from(IssuedPdc p) {
    return new IssuedPdcResponse(
        p.getId(),
        p.getVoucherId(),
        p.getBankAccountId(),
        p.getBranchId(),
        p.getPartyCode(),
        p.getPayeeName(),
        p.getChequeNo(),
        p.getChequeDate(),
        p.getIssueDate(),
        p.getCurrency(),
        p.getAmount(),
        p.getBaseAmount(),
        p.getDepartment(),
        p.getStatus(),
        p.getPresentedOn(),
        p.getPresentationBatchNo(),
        p.getClearedOn(),
        p.getCancelledOn(),
        p.getReplacedOn(),
        p.getReplacesId(),
        p.getReplacedById(),
        p.getRemarks());
  }
}
