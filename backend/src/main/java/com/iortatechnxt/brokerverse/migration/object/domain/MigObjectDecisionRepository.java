package com.iortatechnxt.brokerverse.migration.object.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Decisions of data objects (gate G1). */
public interface MigObjectDecisionRepository extends JpaRepository<MigObjectDecision, Long> {

  /**
   * The decisions of an object, newest first.
   *
   * @param objectCode object
   * @return decisions
   */
  List<MigObjectDecision> findByObjectCodeOrderBySubmittedAtDescIdDesc(String objectCode);

  /**
   * Decisions in a status.
   *
   * @param status status
   * @return decisions
   */
  List<MigObjectDecision> findByStatusOrderBySubmittedAtAsc(MigObjectDecision.Status status);

  /**
   * A decision by number.
   *
   * @param decisionNo number
   * @return decision
   */
  Optional<MigObjectDecision> findByDecisionNo(String decisionNo);

  /**
   * Every decision, newest first (report).
   *
   * @return decisions
   */
  List<MigObjectDecision> findAllByOrderBySubmittedAtDescIdDesc();
}
