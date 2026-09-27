package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link OpsInvoiceOriginSnapshot}. */
public interface OpsInvoiceOriginSnapshotRepository
    extends JpaRepository<OpsInvoiceOriginSnapshot, Long> {

  /**
   * The snapshot of a legacy invoice.
   *
   * @param invoiceId invoice id
   * @return snapshot, empty for BIBS invoices
   */
  Optional<OpsInvoiceOriginSnapshot> findByInvoiceId(Long invoiceId);
}
