package com.iortatechnxt.brokerverse.opsledger.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Invoice movements. */
public interface OpsInvoiceMovementRepository extends JpaRepository<OpsInvoiceMovement, Long> {

  /**
   * Movements of an invoice in posting order.
   *
   * @param invoiceId invoice
   * @return movements
   */
  List<OpsInvoiceMovement> findByInvoiceIdOrderByIdAsc(Long invoiceId);

  /**
   * Movements of one business transaction on an invoice (idempotency).
   *
   * @param sourceModule source module
   * @param sourceRef source reference
   * @param invoiceId invoice
   * @return movements
   */
  List<OpsInvoiceMovement> findBySourceModuleAndSourceRefAndInvoiceIdOrderByIdAsc(
      String sourceModule, String sourceRef, Long invoiceId);

  /**
   * Movements of one business transaction on any invoice.
   *
   * @param sourceModule source module
   * @param sourceRef source reference
   * @return movements
   */
  List<OpsInvoiceMovement> findBySourceModuleAndSourceRefOrderByIdAsc(
      String sourceModule, String sourceRef);

  /**
   * Movements of given types on the invoices of a client since a date, newest first, each with the
   * invoice number and ARN (payment history of the Customer Servicing Facility, BRCSF-005).
   *
   * @param companyId company
   * @param clientCode client code
   * @param types movement types
   * @param from first value date
   * @return rows of movement, invoice number and ARN
   */
  @Query(
      "select m, i.invoiceNo, i.arn from OpsInvoiceMovement m, OpsInvoice i"
          + " where i.id = m.invoiceId and i.companyId = :companyId and i.clientCode = :clientCode"
          + " and m.movementType in :types and m.valueDate >= :from"
          + " order by m.valueDate desc, m.id desc")
  List<Object[]> clientMovements(
      @Param("companyId") Long companyId,
      @Param("clientCode") String clientCode,
      @Param("types") Collection<MovementType> types,
      @Param("from") LocalDate from);
}
