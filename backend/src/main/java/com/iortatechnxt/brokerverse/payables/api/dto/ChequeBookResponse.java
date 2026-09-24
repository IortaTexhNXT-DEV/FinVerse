package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.domain.ChequeBook;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBookStatus;
import java.time.LocalDate;

/**
 * Cheque book view.
 *
 * @param id id
 * @param bankAccountId bank account
 * @param firstNo first leaf
 * @param lastNo last leaf
 * @param nextNo next leaf to issue
 * @param remaining unused leaves
 * @param receivedOn date received
 * @param status status
 */
public record ChequeBookResponse(
    Long id,
    Long bankAccountId,
    long firstNo,
    long lastNo,
    long nextNo,
    long remaining,
    LocalDate receivedOn,
    ChequeBookStatus status) {

  /**
   * Maps an entity.
   *
   * @param b book
   * @return response
   */
  public static ChequeBookResponse from(ChequeBook b) {
    return new ChequeBookResponse(
        b.getId(),
        b.getBankAccountId(),
        b.getFirstNo(),
        b.getLastNo(),
        b.getNextNo(),
        b.remaining(),
        b.getReceivedOn(),
        b.getStatus());
  }
}
