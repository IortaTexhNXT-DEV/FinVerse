package com.iortatechnxt.brokerverse.remittance.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consumptions of remittance deductions by batches (ACSL 2.9.2). */
public interface DeductionApplicationRepository extends JpaRepository<DeductionApplication, Long> {

  /**
   * The consumptions of a batch.
   *
   * @param batchId batch
   * @return consumptions in order
   */
  List<DeductionApplication> findByBatchIdOrderByIdAsc(Long batchId);

  /**
   * The consumptions of a deduction.
   *
   * @param deductionId deduction
   * @return consumptions in order
   */
  List<DeductionApplication> findByDeductionIdOrderByIdAsc(Long deductionId);
}
