package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer responses. */
public interface RenewalInsurerResponseRepository extends JpaRepository<InsurerResponse, Long> {

  /**
   * Responses of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return responses
   */
  List<InsurerResponse> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * Responses of an upload job.
   *
   * @param jobNo job
   * @return responses
   */
  List<InsurerResponse> findByJobNo(String jobNo);
}
