package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Renewal advices of the EB renewal cycles. */
public interface EbRenewalAdviceRepository extends JpaRepository<EbRenewalAdvice, Long> {

  /**
   * The advice of a cycle.
   *
   * @param cycleId cycle
   * @return advice
   */
  Optional<EbRenewalAdvice> findByCycleId(Long cycleId);

  /**
   * The advices of a programme, latest first.
   *
   * @param programmeId programme
   * @return advices
   */
  List<EbRenewalAdvice> findByProgrammeIdOrderBySentAtDesc(Long programmeId);

  /**
   * Advices still waiting for the client's feedback (reminder candidates).
   *
   * @param companyId company, null for every company (job)
   * @return advices, oldest first
   */
  @Query(
      "select r from EbRenewalAdvice r where r.feedbackAt is null"
          + " and (:companyId is null or r.companyId = :companyId) order by r.sentAt, r.id")
  List<EbRenewalAdvice> findAwaitingFeedback(@Param("companyId") Long companyId);
}
