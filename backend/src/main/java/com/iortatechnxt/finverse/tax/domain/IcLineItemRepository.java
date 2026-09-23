package com.iortatechnxt.finverse.tax.domain;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurance Commission schedule mappings. */
public interface IcLineItemRepository extends JpaRepository<IcLineItem, Long> {

  /**
   * All line items of a company.
   *
   * @param companyId company
   * @return items ordered by schedule and line order
   */
  List<IcLineItem> findByCompanyIdOrderByScheduleAscLineOrderAsc(Long companyId);

  /**
   * Line items of one schedule in a status.
   *
   * @param companyId company
   * @param schedule schedule
   * @param status record status
   * @return items in line order
   */
  List<IcLineItem> findByCompanyIdAndScheduleAndRecordStatusOrderByLineOrder(
      Long companyId, IcSchedule schedule, RecordStatus status);

  /**
   * Whether a line exists.
   *
   * @param companyId company
   * @param schedule schedule
   * @param lineCode line code
   * @return true when present
   */
  boolean existsByCompanyIdAndScheduleAndLineCode(
      Long companyId, IcSchedule schedule, String lineCode);
}
