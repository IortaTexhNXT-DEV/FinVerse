package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The legacy block of the invoice 360: the frozen original values of a legacy invoice
 * (DATA_MIGRATION_DESIGN 14.1).
 *
 * @param sourceSystem source system
 * @param legacyInvoiceNo number in the source system
 * @param legacyRef legacy policy reference
 * @param legacyServiceInvoiceNo commission service invoice of legacy
 * @param invoiceDate legacy invoice date
 * @param dueDate legacy due date
 * @param grossPremium original gross premium
 * @param commission original commission
 * @param vatOnCommission original VAT on commission
 * @param commissionRealised commission realised in legacy
 * @param deferredVatOpen deferred output VAT still open
 * @param openBalanceMode legacy gave open balances only
 * @param shares insurer shares
 * @param migrationBatch loading batch
 * @param takenAt time of the snapshot
 * @param lines positions per component
 */
public record OriginSnapshotResponse(
    String sourceSystem,
    String legacyInvoiceNo,
    String legacyRef,
    String legacyServiceInvoiceNo,
    LocalDate invoiceDate,
    LocalDate dueDate,
    BigDecimal grossPremium,
    BigDecimal commission,
    BigDecimal vatOnCommission,
    BigDecimal commissionRealised,
    BigDecimal deferredVatOpen,
    boolean openBalanceMode,
    String shares,
    String migrationBatch,
    Instant takenAt,
    List<OpsInvoiceOriginSnapshot.Line> lines) {

  /**
   * Maps a snapshot (lines loaded).
   *
   * @param s snapshot
   * @return response
   */
  public static OriginSnapshotResponse from(OpsInvoiceOriginSnapshot s) {
    return new OriginSnapshotResponse(
        s.getSourceSystem(),
        s.getLegacyInvoiceNo(),
        s.getLegacyRef(),
        s.getLegacyServiceInvoiceNo(),
        s.getInvoiceDate(),
        s.getDueDate(),
        s.getGrossPremium(),
        s.getCommission(),
        s.getVatOnCommission(),
        s.getCommissionRealised(),
        s.getDeferredVatOpen(),
        s.isOpenBalanceMode(),
        s.getShares(),
        s.getMigrationBatch(),
        s.getTakenAt(),
        List.copyOf(s.getLines()));
  }
}
