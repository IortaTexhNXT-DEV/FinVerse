package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Cumulative adjustments per original invoice (ADJID.028). */
public interface OpsInvoiceAdjustmentTotalRepository
    extends JpaRepository<OpsInvoiceAdjustmentTotal, Long> {

  /**
   * Totals of an original invoice.
   *
   * @param originalInvoiceNo original invoice number
   * @return totals
   */
  Optional<OpsInvoiceAdjustmentTotal> findByOriginalInvoiceNo(String originalInvoiceNo);
}
