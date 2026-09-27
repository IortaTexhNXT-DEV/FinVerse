package com.iortatechnxt.brokerverse.csf.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Caller verifications. */
public interface CsfVerificationRepository extends JpaRepository<CsfVerification, Long> {

  /**
   * The latest verification of a client.
   *
   * @param clientId client
   * @return verification
   */
  @EntityGraph(attributePaths = "checks")
  Optional<CsfVerification> findFirstByClientIdOrderByVerifiedAtDescIdDesc(Long clientId);

  /**
   * Verifications of a client with a result since a time (failed verifications of the day).
   *
   * @param clientId client
   * @param result result
   * @param since start of the day
   * @return count
   */
  long countByClientIdAndResultAndVerifiedAtGreaterThanEqual(
      Long clientId, VerificationResult result, Instant since);

  /**
   * Verifications of a client, newest first.
   *
   * @param clientId client
   * @return verifications
   */
  @EntityGraph(attributePaths = "checks")
  List<CsfVerification> findByClientIdOrderByVerifiedAtDesc(Long clientId);
}
