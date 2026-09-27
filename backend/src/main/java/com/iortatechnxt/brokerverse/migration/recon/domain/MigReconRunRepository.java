package com.iortatechnxt.brokerverse.migration.recon.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Reconciliation runs. */
public interface MigReconRunRepository extends JpaRepository<MigReconRun, Long> {

  /**
   * Runs of a batch, newest first.
   *
   * @param batchId batch
   * @return runs
   */
  List<MigReconRun> findByBatchIdOrderByIdDesc(Long batchId);

  /**
   * Runs of a true-up, newest first.
   *
   * @param trueupId true-up
   * @return runs
   */
  List<MigReconRun> findByTrueupIdOrderByIdDesc(Long trueupId);

  /**
   * A run by number.
   *
   * @param runNo number
   * @return run
   */
  Optional<MigReconRun> findByRunNo(String runNo);

  /**
   * Latest runs of a company.
   *
   * @param companyId company
   * @return runs
   */
  List<MigReconRun> findByCompanyIdOrderByIdDesc(Long companyId);
}
