package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Status, flag and lock history of invoices. */
public interface OpsInvoiceStatusChangeRepository
    extends JpaRepository<OpsInvoiceStatusChange, Long> {

  /**
   * History of an invoice, oldest first.
   *
   * @param invoiceId invoice
   * @return changes
   */
  List<OpsInvoiceStatusChange> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
