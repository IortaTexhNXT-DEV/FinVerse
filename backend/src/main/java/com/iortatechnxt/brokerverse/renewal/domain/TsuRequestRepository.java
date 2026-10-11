package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** TSU requests of the renewal accounts. */
public interface TsuRequestRepository extends JpaRepository<TsuRequest, Long> {

  /**
   * The requests of a renewal, newest first.
   *
   * @param candidateId renewal
   * @return requests
   */
  List<TsuRequest> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * A request by its number.
   *
   * @param companyId company
   * @param requestNo number
   * @return request
   */
  Optional<TsuRequest> findByCompanyIdAndRequestNo(Long companyId, String requestNo);

  /**
   * The requests of a company in a status.
   *
   * @param companyId company
   * @param status status
   * @return requests
   */
  List<TsuRequest> findByCompanyIdAndStatusOrderByIdAsc(Long companyId, String status);
}
