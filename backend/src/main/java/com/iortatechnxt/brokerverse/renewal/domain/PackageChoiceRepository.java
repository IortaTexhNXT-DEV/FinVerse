package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Package choices of migrated policies. */
public interface PackageChoiceRepository extends JpaRepository<PackageChoice, Long> {

  /**
   * Choices of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return choices
   */
  List<PackageChoice> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * Choices in a status.
   *
   * @param status status
   * @return choices
   */
  List<PackageChoice> findByStatusOrderByIdAsc(ApprovalStatus status);
}
