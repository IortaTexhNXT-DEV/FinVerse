package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** BDOI's creation, cancellation and reinstatement records. */
public interface ReceiptRecordRepository extends JpaRepository<ReceiptRecord, Long> {

  /**
   * Records of a list (FRS.CSH.01.03.04, 02.05.01, 03.01.08, 04.01.09), newest first.
   *
   * @param companyId company
   * @param kind record kind
   * @param stages statuses
   * @param criteria other filters, each null for all
   * @param pageable page
   * @return records
   */
  @Query(
      "select r from ReceiptRecord r where r.companyId = :companyId and r.recordKind = :kind"
          + " and r.stage in :stages"
          + " and (:#{#criteria.receiptKind} is null or r.receiptKind = :#{#criteria.receiptKind})"
          + " and (:#{#criteria.receiptType} is null or r.receiptType = :#{#criteria.receiptType})"
          + " and (:#{#criteria.branchId} is null or r.branchId = :#{#criteria.branchId})"
          + " and (:#{#criteria.createdBy} is null or r.createdBy = :#{#criteria.createdBy})"
          + " and (:#{#criteria.from} is null or r.createdAt >= :#{#criteria.from})"
          + " and (:#{#criteria.to} is null or r.createdAt < :#{#criteria.to})"
          + " and (:#{#criteria.recordNo} is null"
          + " or lower(r.recordNo) like concat('%', lower(cast(:#{#criteria.recordNo} as string)), '%'))"
          + " and (:#{#criteria.name} is null"
          + " or lower(coalesce(r.party.clientName, r.party.payorName, '')) like"
          + " concat('%', lower(cast(:#{#criteria.name} as string)), '%')"
          + " or lower(coalesce(r.party.insurerName, '')) like"
          + " concat('%', lower(cast(:#{#criteria.name} as string)), '%'))"
          + " order by r.id desc")
  Page<ReceiptRecord> search(
      @Param("companyId") Long companyId,
      @Param("kind") RecordKind kind,
      @Param("stages") Collection<RecordStage> stages,
      @Param("criteria") RecordCriteria criteria,
      Pageable pageable);

  /**
   * Count and amount per kind, receipt kind, type, status and currency (Cashiering dashboard,
   * FRS.CSH.01.03.02).
   *
   * @param companyId company
   * @param stages statuses counted
   * @return one row per group
   */
  @Query(
      "select new com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository$Tally("
          + "r.recordKind, r.receiptKind, r.receiptType, r.stage, r.tender.currency,"
          + " count(r), coalesce(sum(r.tender.amount), 0))"
          + " from ReceiptRecord r where r.companyId = :companyId and r.stage in :stages"
          + " group by r.recordKind, r.receiptKind, r.receiptType, r.stage, r.tender.currency")
  List<Tally> tally(
      @Param("companyId") Long companyId, @Param("stages") Collection<RecordStage> stages);

  /**
   * Whether a receipt has a cancellation or reinstatement record waiting.
   *
   * @param receiptId receipt
   * @param kinds cancellation and / or reinstatement
   * @param stages open statuses
   * @return true when one is waiting
   */
  boolean existsByReceiptIdAndRecordKindInAndStageIn(
      Long receiptId, Collection<RecordKind> kinds, Collection<RecordStage> stages);

  /**
   * Records of a receipt, oldest first.
   *
   * @param receiptId receipt
   * @return records
   */
  List<ReceiptRecord> findByReceiptIdOrderByIdAsc(Long receiptId);

  /**
   * The records a cashier created on a day (FRS.CSH.02.01.16).
   *
   * @param companyId company
   * @param kind record kind
   * @param createdBy cashier
   * @param from start of the day
   * @param to start of the next day
   * @return creation records, oldest first
   */
  @Query(
      "select r from ReceiptRecord r where r.companyId = :companyId and r.createdBy = :createdBy"
          + " and r.recordKind = :kind"
          + " and r.createdAt >= :from and r.createdAt < :to order by r.id")
  List<ReceiptRecord> dayList(
      @Param("companyId") Long companyId,
      @Param("kind") RecordKind kind,
      @Param("createdBy") String createdBy,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Filters of a record list; null means all.
   *
   * @param receiptKind AR or OR
   * @param receiptType AR or OR type
   * @param branchId receipting branch
   * @param createdBy creator (user name)
   * @param from created from
   * @param to created before
   * @param recordNo part of the record number
   * @param name part of the client, payor or insurer name
   */
  record RecordCriteria(
      ReceiptKind receiptKind,
      String receiptType,
      Long branchId,
      String createdBy,
      Instant from,
      Instant to,
      String recordNo,
      String name) {}

  /**
   * One dashboard group.
   *
   * @param kind record kind
   * @param receiptKind AR or OR
   * @param receiptType AR or OR type
   * @param stage status
   * @param currency currency
   * @param count records
   * @param amount total paid amount
   */
  record Tally(
      RecordKind kind,
      ReceiptKind receiptKind,
      String receiptType,
      RecordStage stage,
      String currency,
      long count,
      BigDecimal amount) {}
}
