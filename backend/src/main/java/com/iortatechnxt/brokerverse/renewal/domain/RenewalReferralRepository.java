package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Transfer requests for New Business opportunities. */
public interface RenewalReferralRepository extends JpaRepository<RenewalReferral, Long> {

  /**
   * Requests of a company, newest first.
   *
   * @param companyId company
   * @return requests
   */
  List<RenewalReferral> findByCompanyIdOrderByIdDesc(Long companyId);

  /**
   * Requests of a renewal, newest first.
   *
   * @param candidateId renewal
   * @return requests
   */
  List<RenewalReferral> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * An open request of a renewal to a unit.
   *
   * @param candidateId renewal
   * @param toUnit receiving unit
   * @param statuses open statuses
   * @return request
   */
  Optional<RenewalReferral> findFirstByCandidateIdAndToUnitAndStatusIn(
      Long candidateId, String toUnit, Collection<ReferralStatus> statuses);
}
