package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Hold cover requests of the renewal accounts. */
public interface HoldCoverAskRepository extends JpaRepository<HoldCoverAsk, Long> {

  /**
   * The requests of a renewal, newest first.
   *
   * @param candidateId renewal
   * @return requests
   */
  List<HoldCoverAsk> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * A request by its reference number.
   *
   * @param companyId company
   * @param requestNo reference number
   * @return request
   */
  Optional<HoldCoverAsk> findByCompanyIdAndRequestNo(Long companyId, String requestNo);
}
