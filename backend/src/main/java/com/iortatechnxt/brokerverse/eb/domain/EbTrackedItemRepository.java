package com.iortatechnxt.brokerverse.eb.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Tracked items of the EB programmes. */
public interface EbTrackedItemRepository
    extends JpaRepository<EbTrackedItem, Long>, JpaSpecificationExecutor<EbTrackedItem> {

  /**
   * Pending items past their due date (follow-up candidates).
   *
   * @param today business date
   * @return items, oldest due first
   */
  @Query(
      "select i from EbTrackedItem i where i.status ="
          + " com.iortatechnxt.brokerverse.eb.domain.EbItemStatus.PENDING"
          + " and i.dueDate < :today order by i.dueDate, i.id")
  List<EbTrackedItem> findPendingPastDue(@Param("today") LocalDate today);

  /**
   * The items of a programme, newest first.
   *
   * @param programmeId programme
   * @return items
   */
  List<EbTrackedItem> findByProgrammeIdOrderByIdDesc(Long programmeId);

  /**
   * The item of a type for an account (one contract item per account).
   *
   * @param itemType type
   * @param accountArn ARN
   * @return true when present
   */
  boolean existsByItemTypeAndAccountArn(String itemType, String accountArn);
}
