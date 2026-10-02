package com.iortatechnxt.brokerverse.acsl.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accounts of the GL-SL reconciliation runs (ACSL 2.13.2). */
public interface GlSlReconRepository extends JpaRepository<GlSlRecon, Long> {

  /**
   * Accounts of a run.
   *
   * @param runId run
   * @return accounts by code
   */
  List<GlSlRecon> findByRunIdOrderByAccountCode(Long runId);
}
