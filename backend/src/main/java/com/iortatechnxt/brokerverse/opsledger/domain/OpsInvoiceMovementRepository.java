package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
