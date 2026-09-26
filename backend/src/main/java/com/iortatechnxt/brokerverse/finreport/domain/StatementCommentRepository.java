package com.iortatechnxt.brokerverse.finreport.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Schedule commentary. */
public interface StatementCommentRepository extends JpaRepository<StatementComment, Long> {

  /**
   * The comments of a schedule for a month.
   *
   * @param companyId company
   * @param scheduleCode schedule
   * @param period month {@code yyyy-MM}
   * @return comments
   */
  List<StatementComment> findByCompanyIdAndScheduleCodeAndPeriod(
      Long companyId, String scheduleCode, String period);

  /**
   * One comment.
   *
   * @param companyId company
   * @param scheduleCode schedule
   * @param period month
   * @param rowKey row
   * @return comment
   */
  Optional<StatementComment> findByCompanyIdAndScheduleCodeAndPeriodAndRowKey(
      Long companyId, String scheduleCode, String period, String rowKey);
}
