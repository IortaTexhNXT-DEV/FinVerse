package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client feedback of the EB cycles. */
public interface EbFeedbackRepository extends JpaRepository<EbFeedback, Long> {

  /**
   * The feedback of a programme, latest first.
   *
   * @param programmeId programme
   * @return feedback
   */
  List<EbFeedback> findByProgrammeIdOrderByReceivedOnDescIdDesc(Long programmeId);

  /**
   * Whether a cycle has feedback.
   *
   * @param cycleId cycle
   * @return true when recorded
   */
  boolean existsByCycleId(Long cycleId);
}
