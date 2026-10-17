package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A movement of an invoice component (RMTID.038 payment and remittance history).
 *
 * @param id id
 * @param type movement type
 * @param component component
 * @param amount signed amount
 * @param sourceModule source module
 * @param sourceRef source reference
 * @param arNo AR number
 * @param orNo OR number
 * @param batchNo batch number
 * @param valueDate value date
 * @param journalBatchNo GL journal
 * @param remarks remarks
 * @param postedAt posted at
 * @param postedBy posted by
 */
public record MovementResponse(
    Long id,
    MovementType type,
    LedgerComponent component,
    BigDecimal amount,
    String sourceModule,
    String sourceRef,
    String arNo,
    String orNo,
    String batchNo,
    LocalDate valueDate,
    String journalBatchNo,
    String remarks,
    Instant postedAt,
    String postedBy) {

  /**
   * Maps a movement.
   *
   * @param m movement
   * @return response
   */
  public static MovementResponse from(OpsInvoiceMovement m) {
    return new MovementResponse(
        m.getId(),
        m.getMovementType(),
        m.getComponent(),
        m.getAmount(),
        m.getSourceModule(),
        m.getSourceRef(),
        m.getArNo(),
        m.getOrNo(),
        m.getBatchNo(),
        m.getValueDate(),
        m.getJournalBatchNo(),
        m.getRemarks(),
        m.getPostedAt(),
        m.getPostedBy());
  }
}
