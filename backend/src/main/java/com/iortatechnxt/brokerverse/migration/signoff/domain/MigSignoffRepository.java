package com.iortatechnxt.brokerverse.migration.signoff.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Sign-offs of the gates. */
public interface MigSignoffRepository extends JpaRepository<MigSignoff, Long> {

  /**
   * Sign-offs of a batch.
   *
   * @param batchId batch
   * @return sign-offs, oldest first
   */
  List<MigSignoff> findByBatchIdOrderByIdAsc(Long batchId);

  /**
   * Sign-offs of an object.
   *
   * @param companyId company
   * @param objectCode object
   * @return sign-offs, oldest first
   */
  List<MigSignoff> findByCompanyIdAndObjectCodeOrderByIdAsc(Long companyId, String objectCode);

  /**
   * Every sign-off of a company.
   *
   * @param companyId company
   * @return sign-offs, oldest first
   */
  List<MigSignoff> findByCompanyIdOrderByIdAsc(Long companyId);
}
