package com.iortatechnxt.brokerverse.journal.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence for {@link JournalBatch}. Dynamic searches use {@link JournalSpecifications}. */
public interface JournalBatchRepository
    extends JpaRepository<JournalBatch, Long>, JpaSpecificationExecutor<JournalBatch> {

  /**
   * Finds a batch with its lines (the aggregate is always used whole).
   *
   * @param id id
   * @return batch if present
   */
  @Override
  @EntityGraph(attributePaths = "lines", type = EntityGraphType.LOAD)
  Optional<JournalBatch> findById(Long id);

  /**
   * Finds a batch by number.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return batch if present
   */
  Optional<JournalBatch> findByCompanyIdAndBatchNo(Long companyId, String batchNo);

  /**
   * Finds a non-cancelled system journal by its source idempotency key.
   *
   * @param companyId company
   * @param sourceModule module
   * @param sourceReference source key
   * @param excluded statuses to ignore
   * @return batch if already generated
   */
  Optional<JournalBatch> findFirstByCompanyIdAndSourceModuleAndSourceReferenceAndStatusNotIn(
      Long companyId,
      String sourceModule,
      String sourceReference,
      Collection<JournalStatus> excluded);

  /**
   * Counts batches in given statuses whose value date falls within a range.
   *
   * @param companyId company
   * @param statuses statuses
   * @param from start
   * @param to end
   * @return count
   */
  long countByCompanyIdAndStatusInAndValueDateBetween(
      Long companyId, Collection<JournalStatus> statuses, LocalDate from, LocalDate to);

  /**
   * Counts batches in given statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @return count
   */
  long countByCompanyIdAndStatusIn(Long companyId, Collection<JournalStatus> statuses);
}
