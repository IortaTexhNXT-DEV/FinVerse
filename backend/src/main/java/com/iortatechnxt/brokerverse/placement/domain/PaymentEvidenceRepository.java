package com.iortatechnxt.brokerverse.placement.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Payment gate evidence. */
public interface PaymentEvidenceRepository extends JpaRepository<PaymentEvidence, Long> {

  /**
   * Evidence of an account, oldest first.
   *
   * @param arn Account Reference Number
   * @return evidence
   */
  List<PaymentEvidence> findByArnOrderByIdAsc(String arn);

  /**
   * Whether a source reference is already recorded for an account.
   *
   * @param arn ARN
   * @param source source
   * @param reference reference
   * @return true when recorded
   */
  boolean existsByArnAndSourceAndReference(String arn, String source, String reference);
}
