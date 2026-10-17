package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Approval matrices. */
public interface SbmApprovalMatrixRepository extends JpaRepository<SbmApprovalMatrix, Long> {

  /**
   * Levels of a company.
   *
   * @param companyId company
   * @return levels
   */
  List<SbmApprovalMatrix> findByCompanyIdOrderByDocumentAscSegmentAscTsiFromAscLevelAsc(
      Long companyId);

  /**
   * Levels of a document in a status.
   *
   * @param companyId company
   * @param document IAAF or TOR
   * @param status ACTIVE
   * @return levels
   */
  List<SbmApprovalMatrix> findByCompanyIdAndDocumentAndRecordStatusOrderByLevelAsc(
      Long companyId, String document, RecordStatus status);
}
