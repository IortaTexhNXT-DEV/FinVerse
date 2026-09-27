package com.iortatechnxt.brokerverse.booking.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Booked invoices. */
public interface BookedInvoiceRepository
    extends JpaRepository<BookedInvoice, Long>, JpaSpecificationExecutor<BookedInvoice> {

  /**
   * An invoice by number.
   *
   * @param invoiceNo invoice number
   * @return invoice
   */
  Optional<BookedInvoice> findByInvoiceNo(String invoiceNo);

  /**
   * Every invoice of an account (booked, scheduled and cancelled years), by year then creation.
   *
   * @param arn Account Reference Number
   * @return invoices
   */
  List<BookedInvoice> findByArnOrderByPolicyYearAscIdAsc(String arn);

  /**
   * Whether a transaction of an account is already invoiced (booking key, BRNB.076).
   *
   * @param arn Account Reference Number
   * @param transactionNo transaction
   * @return true when it exists
   */
  boolean existsByArnAndTransactionNo(String arn, String transactionNo);

  /**
   * The invoice that already carries an insurer billing number (BRID-020 duplicate block).
   *
   * @param companyId company
   * @param insurerCode lead insurer
   * @param insurerBillingNo billing number
   * @return invoice, if any
   */
  Optional<BookedInvoice> findFirstByCompanyIdAndFactsInsurerCodeAndInsurerBillingNo(
      Long companyId, String insurerCode, String insurerBillingNo);

  /**
   * Scheduled policy years that start on or before a date (multi-year, BRNB.112).
   *
   * @param status SCHEDULED
   * @param date business date
   * @return invoices to book
   */
  List<BookedInvoice> findByStatusAndInceptionDateLessThanEqualOrderByIdAsc(
      InvoiceStatus status, LocalDate date);

  /**
   * Invoices of a client, newest first (client 360 view).
   *
   * @param clientId client
   * @return invoices
   */
  List<BookedInvoice> findByFactsClientIdOrderByIdDesc(Long clientId);

  /**
   * Invoices of a company booked on a date.
   *
   * @param companyId company
   * @param status BOOKED
   * @param date booking date
   * @return count
   */
  long countByCompanyIdAndStatusAndBookingDate(
      Long companyId, InvoiceStatus status, LocalDate date);
}
