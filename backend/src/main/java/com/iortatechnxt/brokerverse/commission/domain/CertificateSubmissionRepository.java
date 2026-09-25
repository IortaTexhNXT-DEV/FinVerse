package com.iortatechnxt.brokerverse.commission.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** BIR certificate submissions (CMRID.015). */
public interface CertificateSubmissionRepository
    extends JpaRepository<CertificateSubmission, Long> {

  /**
   * Submissions of a company, newest first.
   *
   * @param companyId company
   * @param stage stage, null for all
   * @param pageable page
   * @return submissions
   */
  @Query(
      """
      select c from CertificateSubmission c where c.companyId = :companyId
        and (:stage is null or c.stage = :stage)
      order by c.id desc
      """)
  Page<CertificateSubmission> search(
      @Param("companyId") Long companyId, @Param("stage") String stage, Pageable pageable);

  /**
   * A submission with its ORs.
   *
   * @param id id
   * @return submission
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "receipts")
  Optional<CertificateSubmission> findWithReceiptsById(Long id);

  /**
   * Submissions of a company in a stage.
   *
   * @param companyId company
   * @param stage stage
   * @return count
   */
  long countByCompanyIdAndStage(Long companyId, String stage);
}
