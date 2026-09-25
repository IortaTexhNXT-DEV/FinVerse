package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port: read the unapplied payment items of Cashiering (BRCLXN.034-036, 040; COLLECTIONS_DESIGN
 * section 9). Collections shows them to collectors and joins its own dispositions by {@link
 * UnappliedView#unappliedRef()}; the items are never copied. Implemented by cashiering; the default
 * adapter ({@code EmptyUnappliedDirectory}) lists nothing, so the Collections screens work without
 * cashiering.
 */
public interface UnappliedDirectory {

  /**
   * Open unapplied items (balance above zero) of a company.
   *
   * @param companyId company
   * @param filter criteria
   * @param pageable page and sort
   * @return items
   */
  Page<UnappliedView> open(Long companyId, UnappliedFilter filter, Pageable pageable);

  /**
   * One unapplied item, also when fully disposed.
   *
   * @param unappliedRef item reference (Cashiering key)
   * @return item, empty when unknown
   */
  Optional<UnappliedView> find(String unappliedRef);

  /**
   * History of an item: intake, matching, dispositions, applications, refunds (BRCLXN.040).
   *
   * @param unappliedRef item reference
   * @return events, oldest first; empty when unknown
   */
  List<UnappliedEvent> history(String unappliedRef);

  /**
   * Criteria of the collector list (BRCLXN.034-036). Null fields do not filter.
   *
   * @param text free text over reference, payor, transaction and check number
   * @param clientCode matched client
   * @param salesUnit marketing unit the money belongs to
   * @param tab Cashiering tab (e.g. UNMATCHED, EXCESS, FOR_DISPOSITION)
   * @param paidFrom payment date from
   * @param paidTo payment date to
   */
  record UnappliedFilter(
      String text,
      String clientCode,
      String salesUnit,
      String tab,
      LocalDate paidFrom,
      LocalDate paidTo) {

    /**
     * No criteria.
     *
     * @return filter matching every open item
     */
    public static UnappliedFilter all() {
      return new UnappliedFilter(null, null, null, null, null, null);
    }
  }

  /**
   * An unapplied payment item as Collections shows it (BRCLXN.034/035).
   *
   * @param unappliedRef item reference (Cashiering key)
   * @param companyId company
   * @param paymentDate payment (value) date
   * @param paymentFileName payment file the payment came from, may be null
   * @param transactionNo bank transaction number, may be null
   * @param currency currency
   * @param amount original amount
   * @param balance amount still unapplied
   * @param paymentType payment type (CASH, CHECK, BANK_TRANSFER, ...)
   * @param payor payor name
   * @param bankCode bank, may be null
   * @param checkNo check number, may be null
   * @param reference payment reference given by the payor, may be null
   * @param matchedClientCode client matched by Cashiering, may be null
   * @param matchedInvoiceNo invoice matched by Cashiering, may be null
   * @param salesUnit marketing unit, may be null
   * @param cashieringTab Cashiering tab of the item
   * @param dispositionStatus status of the Cashiering disposition, may be null
   */
  record UnappliedView(
      String unappliedRef,
      Long companyId,
      LocalDate paymentDate,
      String paymentFileName,
      String transactionNo,
      String currency,
      BigDecimal amount,
      BigDecimal balance,
      String paymentType,
      String payor,
      String bankCode,
      String checkNo,
      String reference,
      String matchedClientCode,
      String matchedInvoiceNo,
      String salesUnit,
      String cashieringTab,
      String dispositionStatus) {}

  /**
   * One event of an item's history (BRCLXN.040).
   *
   * @param at time
   * @param event event code (RECEIVED, MATCHED, DISPOSITION, APPLIED, REFUNDED, ...)
   * @param description what happened
   * @param amount amount concerned, may be null
   * @param by user or SYSTEM
   * @param reference related document (AR, disposition, invoice), may be null
   */
  record UnappliedEvent(
      Instant at,
      String event,
      String description,
      BigDecimal amount,
      String by,
      String reference) {}
}
