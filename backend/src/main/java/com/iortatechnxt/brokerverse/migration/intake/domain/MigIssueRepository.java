package com.iortatechnxt.brokerverse.migration.intake.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Data-quality issues. */
public interface MigIssueRepository extends JpaRepository<MigIssue, Long> {

  /**
   * Issues of a batch, paged.
   *
   * @param batchId batch
   * @param pageable page
   * @return issues
   */
  Page<MigIssue> findByBatchIdOrderByIdAsc(Long batchId, Pageable pageable);

  /**
   * Issues of rows.
   *
   * @param rowIds rows
   * @return issues
   */
  List<MigIssue> findByStageRowIdIn(Collection<Long> rowIds);

  /**
   * Issues of a row.
   *
   * @param rowId row
   * @return issues
   */
  List<MigIssue> findByStageRowId(Long rowId);

  /**
   * Issues of a batch with a rule.
   *
   * @param batchId batch
   * @param ruleCode rule
   * @return issues
   */
  List<MigIssue> findByBatchIdAndRuleCode(Long batchId, String ruleCode);

  /**
   * Removes the open issues of rows before they are validated again.
   *
   * @param rowIds rows
   * @param open the OPEN resolution
   * @return removed
   */
  @Modifying
  @Query("delete from MigIssue i where i.stageRowId in ?1 and i.resolution = ?2")
  int deleteOpenOf(Collection<Long> rowIds, MigIssue.Resolution open);
}
