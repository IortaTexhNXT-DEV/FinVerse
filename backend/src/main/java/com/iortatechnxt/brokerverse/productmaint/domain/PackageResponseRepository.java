package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Package insurer responses. */
public interface PackageResponseRepository extends JpaRepository<PackageInsurerResponse, Long> {

  /**
   * Responses of a round in the order the insurers were approached.
   *
   * @param roundId round
   * @return responses
   */
  List<PackageInsurerResponse> findByRoundIdOrderById(Long roundId);

  /**
   * Responses of several rounds.
   *
   * @param roundIds rounds
   * @return responses
   */
  List<PackageInsurerResponse> findByRoundIdInOrderById(Collection<Long> roundIds);

  /**
   * The response of one insurer in a round.
   *
   * @param roundId round
   * @param insurerCode insurer
   * @return response
   */
  Optional<PackageInsurerResponse> findByRoundIdAndInsurerCode(Long roundId, String insurerCode);
}
