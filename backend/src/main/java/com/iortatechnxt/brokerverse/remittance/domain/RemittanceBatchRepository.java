package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Remittance batches. */
public interface RemittanceBatchRepository
    extends JpaRepository<RemittanceBatch, Long>, JpaSpecificationExecutor<RemittanceBatch> {

  /**
   * A batch with its lines.
   *
   * @param id id
   * @return batch
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "lines")
  Optional<RemittanceBatch> findWithLinesById(Long id);

  /**
   * A batch with its lines, by number.
   *
   * @param batchNo batch number
   * @return batch
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "lines")
  Optional<RemittanceBatch> findByBatchNo(String batchNo);

  /**
   * Batches of a company in some stages.
   *
   * @param companyId company
   * @param stages stages
   * @return count
   */
  long countByCompanyIdAndStageIn(Long companyId, Collection<BatchStage> stages);

  /**
   * Batches in some stages, oldest first (pending approvals).
   *
   * @param stages stages
   * @return batches
   */
  List<RemittanceBatch> findByStageInOrderByIdAsc(Collection<BatchStage> stages);
}
