package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Account funding requests (DIS 2.17). */
public interface FundingRequestRepository extends JpaRepository<FundingRequest, Long> {

  /**
   * Requests of a company in some stages, newest first.
   *
   * @param companyId company
   * @param stages stages
   * @param pageable page
   * @return requests
   */
  Page<FundingRequest> findByCompanyIdAndStageInOrderByIdDesc(
      Long companyId, Collection<FundingStage> stages, Pageable pageable);

  /**
   * Requests in some stages, oldest first (pending approvals).
   *
   * @param stages stages
   * @return requests
   */
  List<FundingRequest> findByStageInOrderByIdAsc(Collection<FundingStage> stages);

  /**
   * Requests of a company in some stages.
   *
   * @param companyId company
   * @param stages stages
   * @return count
   */
  long countByCompanyIdAndStageIn(Long companyId, Collection<FundingStage> stages);
}
