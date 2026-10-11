package com.iortatechnxt.brokerverse.collections.promise.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Promises to pay (BRCLXN.055). */
public interface PaymentPromiseRepository extends JpaRepository<PaymentPromise, Long> {

  /**
   * Promises of a company by status and date range, searched by invoice, account, client or
   * assured.
   *
   * @param companyId company
   * @param statuses statuses
   * @param from first promised date
   * @param to last promised date
   * @param like lower-case pattern, {@code %} for all
   * @param pageable page
   * @return promises
   */
  @Query(
      "select p from PaymentPromise p where p.companyId = :companyId and p.status in :statuses"
          + " and p.promisedDate between :from and :to"
          + " and (lower(p.invoiceNo) like :like or lower(p.arn) like :like"
          + " or lower(p.clientCode) like :like or lower(p.assuredName) like :like)")
  Page<PaymentPromise> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<PromiseStatus> statuses,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      @Param("like") String like,
      Pageable pageable);

  /**
   * Promises of an invoice, newest first.
   *
   * @param invoiceNo invoice
   * @return promises
   */
  List<PaymentPromise> findByInvoiceNoOrderByIdDesc(String invoiceNo);

  /**
   * Promises of an invoice in a status.
   *
   * @param invoiceNo invoice
   * @param status status
   * @return promises
   */
  List<PaymentPromise> findByInvoiceNoAndStatus(String invoiceNo, PromiseStatus status);

  /**
   * Ids of the open promises whose promised date is on or before a date, oldest first.
   *
   * @param status OPEN
   * @param date last promised date
   * @return ids
   */
  @Query(
      "select p.id from PaymentPromise p where p.status = :status and p.promisedDate <= :date"
          + " order by p.promisedDate, p.id")
  List<Long> idsDueBy(@Param("status") PromiseStatus status, @Param("date") LocalDate date);

  /**
   * Number of promises per invoice in a status (escalation basis BROKEN_PROMISES_COUNT).
   *
   * @param companyId company
   * @param status status
   * @return rows of invoice number and count
   */
  @Query(
      "select p.invoiceNo, count(p) from PaymentPromise p where p.companyId = :companyId"
          + " and p.status = :status group by p.invoiceNo")
  List<Object[]> countByInvoice(
      @Param("companyId") Long companyId, @Param("status") PromiseStatus status);

  /**
   * Invoices of a company with a promise in a status (escalation basis NO_COMMITMENT_BY_DAY).
   *
   * @param companyId company
   * @param status status
   * @return invoice numbers
   */
  @Query(
      "select distinct p.invoiceNo from PaymentPromise p where p.companyId = :companyId"
          + " and p.status = :status")
  List<String> invoicesWith(
      @Param("companyId") Long companyId, @Param("status") PromiseStatus status);
}
