package com.iortatechnxt.brokerverse.booking.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Incentive evaluations. */
public interface IncentiveEvaluationRepository extends JpaRepository<IncentiveEvaluation, Long> {

  /**
   * The evaluations of a transaction, oldest first.
   *
   * @param invoiceNo root invoice
   * @return evaluations
   */
  List<IncentiveEvaluation> findByInvoiceNoOrderByIdAsc(String invoiceNo);
}
