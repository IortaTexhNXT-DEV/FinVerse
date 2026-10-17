package com.iortatechnxt.brokerverse.booking.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Daily service-invoice batches (G09). */
public interface SiBatchRepository extends JpaRepository<SiBatch, Long> {

  /**
   * The batches of an insurer and date, the last one first.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param batchDate date
   * @return batches
   */
  List<SiBatch> findByCompanyIdAndInsurerCodeAndBatchDateOrderBySequenceNoDesc(
      Long companyId, String insurerCode, LocalDate batchDate);

  /**
   * Batches of a company over a period, newest first.
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return batches
   */
  List<SiBatch> findByCompanyIdAndBatchDateBetweenOrderByBatchDateDescInsurerNameAsc(
      Long companyId, LocalDate from, LocalDate to);
}
