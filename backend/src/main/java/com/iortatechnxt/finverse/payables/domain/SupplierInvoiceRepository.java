package com.iortatechnxt.finverse.payables.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link SupplierInvoice}. */
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {

  /**
   * Loads an invoice with its lines.
   *
   * @param id id
   * @return invoice
   */
  @EntityGraph(
      type = EntityGraph.EntityGraphType.LOAD,
      attributePaths = {"lines"})
  Optional<SupplierInvoice> findWithLinesById(Long id);

  /**
   * Checks for a live invoice with the same supplier invoice number (duplicate billing).
   *
   * @param companyId company
   * @param partyId supplier
   * @param supplierInvoiceNo supplier invoice number
   * @param statuses statuses considered live
   * @return true when a duplicate exists
   */
  boolean existsByCompanyIdAndPartyIdAndSupplierInvoiceNoAndStatusIn(
      Long companyId, Long partyId, String supplierInvoiceNo, Collection<InvoiceStatus> statuses);

  /**
   * Checks whether a supplier invoice number was captured in a company.
   *
   * @param companyId company
   * @param supplierInvoiceNo supplier invoice number
   * @return true when present
   */
  boolean existsByCompanyIdAndSupplierInvoiceNo(Long companyId, String supplierInvoiceNo);

  /**
   * Searches invoices.
   *
   * @param companyId company
   * @param status status or null
   * @param partyCode supplier code or null
   * @param from invoice date from
   * @param to invoice date to
   * @param pageable page
   * @return page of invoices
   */
  @Query(
      """
      select i from SupplierInvoice i
      where i.companyId = :companyId
        and (:status is null or i.status = :status)
        and (:partyCode is null or i.partyCode = :partyCode)
        and i.invoiceDate between :from and :to
      """)
  Page<SupplierInvoice> search(
      @Param("companyId") Long companyId,
      @Param("status") InvoiceStatus status,
      @Param("partyCode") String partyCode,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);

  /**
   * Invoices in one status across companies (approval inbox).
   *
   * @param status status
   * @return invoices, oldest first
   */
  List<SupplierInvoice> findByStatusOrderById(InvoiceStatus status);
}
