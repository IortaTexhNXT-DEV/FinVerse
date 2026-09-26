package com.iortatechnxt.brokerverse.collections.worklist.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.AssignmentKind;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Assignments of collection items (BRCLXN.052). */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

  /**
   * The assignment history of an item, newest first.
   *
   * @param itemId item
   * @return assignments
   */
  List<Assignment> findByItemIdOrderByIdDesc(Long itemId);

  /**
   * The latest assignment of an item.
   *
   * @param itemId item
   * @return assignment
   */
  Optional<Assignment> findFirstByItemIdOrderByIdDesc(Long itemId);

  /**
   * Temporary assignments whose last day has passed and that are not ended.
   *
   * @param companyId company
   * @param kind TEMPORARY
   * @param before last day before this date
   * @return assignments, oldest first
   */
  List<Assignment> findByCompanyIdAndKindAndValidToBeforeAndRevertedAtIsNullOrderByIdAsc(
      Long companyId, AssignmentKind kind, LocalDate before);
}
