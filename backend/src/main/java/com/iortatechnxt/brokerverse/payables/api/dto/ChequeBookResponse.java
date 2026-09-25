package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.domain.ChequeBook;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBookStatus;
import java.time.Instant;
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
 * @param editedBy user who edited the beginning series (DIS 2.23.2), null when never edited
 * @param editedAt time of that edit
 * @param previousRange range before the edit
 */
public record ChequeBookResponse(
    Long id,
    Long bankAccountId,
    long firstNo,
    long lastNo,
    long nextNo,
    long remaining,
    LocalDate receivedOn,
    ChequeBookStatus status,
    String editedBy,
    Instant editedAt,
    String previousRange) {

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
        b.getStatus(),
        b.getEditedBy(),
        b.getEditedAt(),
        b.getPreviousRange());
  }
}
