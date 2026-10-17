package com.iortatechnxt.brokerverse.report.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ReportBatch}. */
public interface ReportBatchRepository extends JpaRepository<ReportBatch, Long> {

  /**
   * A batch with its items.
   *
   * @param id id
   * @return batch if present
   */
  @Override
  @EntityGraph(attributePaths = "items", type = EntityGraphType.LOAD)
  Optional<ReportBatch> findById(Long id);

  /**
   * Batches of a user, newest first.
   *
   * @param createdBy user
   * @param pageable page
   * @return batches
   */
  Page<ReportBatch> findByCreatedByOrderByIdDesc(String createdBy, Pageable pageable);
}
