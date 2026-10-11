package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Disposition history. */
public interface RenewalDispositionRepository extends JpaRepository<Disposition, Long> {

  /**
   * Dispositions of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return rows
   */
  List<Disposition> findByCandidateIdOrderByIdDesc(Long candidateId);
}
