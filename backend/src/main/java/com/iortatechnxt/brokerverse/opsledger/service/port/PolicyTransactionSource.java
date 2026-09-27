package com.iortatechnxt.brokerverse.opsledger.service.port;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Port: the business transactions another module raised on the invoices of a policy (endorsement
 * and cancellation requests of the adjustment module, with the refunds they gave), for the policy
 * transaction history. The ledger merges them with the invoices of the family: a transaction that
 * booked an invoice is shown on that invoice's row; any other is a row of its own.
 */
public interface PolicyTransactionSource {

  /**
   * The transactions raised on some invoices.
   *
   * @param invoiceNos invoices of the family
   * @return transactions, in any order
   */
  List<SourcedTransaction> transactionsFor(Collection<String> invoiceNos);

  /** Kind of a transaction row. */
  enum Kind {
    /** Original booking of the policy. */
    BOOKING,
    /** Financial or non-financial endorsement. */
    ENDORSEMENT,
    /** Flat, flat retain-DST or partial cancellation. */
    CANCELLATION,
    /** Internal adjustment, commission change or write-off. */
    ADJUSTMENT,
    /** Refund to the client of the payments made in excess by a decrease or cancellation. */
    REFUND
  }

  /**
   * A transaction of another module.
   *
   * @param kind kind of row
   * @param reference business reference (e.g. the request number)
   * @param recordId record id (screen link), may be null
   * @param invoiceNo invoice the transaction was raised on
   * @param bookedInvoiceNo invoice its posting booked, null when none
   * @param date transaction date (posting date once posted, else the date raised)
   * @param effectiveDate effective date, may be null
   * @param typeLabel business label of the type (e.g. "Financial – Change of Cover")
   * @param detail second line (e.g. the request type), may be null
   * @param changes signed change per component
   * @param status status code
   * @param statusLabel status as users read it
   * @param posted whether the transaction is posted (it then moves the position)
   * @param journalBatchNos GL journals it posted
   */
  record SourcedTransaction(
      Kind kind,
      String reference,
      Long recordId,
      String invoiceNo,
      String bookedInvoiceNo,
      LocalDate date,
      LocalDate effectiveDate,
      String typeLabel,
      String detail,
      Map<LedgerComponent, BigDecimal> changes,
      String status,
      String statusLabel,
      boolean posted,
      List<String> journalBatchNos) {

    /** Defensive copies. */
    public SourcedTransaction {
      changes =
          changes == null || changes.isEmpty()
              ? Map.of()
              : Collections.unmodifiableMap(new EnumMap<>(changes));
      journalBatchNos = journalBatchNos == null ? List.of() : List.copyOf(journalBatchNos);
    }
  }
}
