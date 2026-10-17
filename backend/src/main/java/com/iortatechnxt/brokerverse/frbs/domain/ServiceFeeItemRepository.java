package com.iortatechnxt.brokerverse.frbs.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Invoices of the service-fee lines (FRBS 2.10.0). */
public interface ServiceFeeItemRepository extends JpaRepository<ServiceFeeItem, Long> {

  /**
   * The invoices of a run.
   *
   * @param runId run
   * @return invoices
   */
  List<ServiceFeeItem> findByRunIdOrderByInvoiceNoAsc(Long runId);

  /**
   * The invoices of a line.
   *
   * @param lineId line
   * @return invoices
   */
  List<ServiceFeeItem> findByLineIdOrderByInvoiceNoAsc(Long lineId);
}
