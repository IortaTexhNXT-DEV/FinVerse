package com.iortatechnxt.brokerverse.commission.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Commission billings to insurers. */
public interface DpBillingRepository extends JpaRepository<DpBilling, Long> {

  /**
   * Billings of a company, newest first.
   *
   * @param companyId company
   * @param stage stage, null for all
   * @param insurer insurer, null for all
   * @param pageable page
   * @return billings
   */
  @Query(
      """
      select b from DpBilling b where b.companyId = :companyId
        and (:stage is null or b.stage = :stage)
        and (:insurer is null or b.insurerCode = :insurer)
      order by b.id desc
      """)
  Page<DpBilling> search(
      @Param("companyId") Long companyId,
      @Param("stage") String stage,
      @Param("insurer") String insurer,
      Pageable pageable);

  /**
   * A billing by number.
   *
   * @param billingNo billing number
   * @return billing
   */
  Optional<DpBilling> findByBillingNo(String billingNo);

  /**
   * Billings awaiting the insurer past their due date and not flagged yet (CMRID.011).
   *
   * @param stage awaiting stage
   * @param date business date
   * @return billings
   */
  List<DpBilling> findByStageAndSlaDueBeforeAndOverdueAlertedFalseOrderByIdAsc(
      String stage, LocalDate date);

  /**
   * Billings of a company in a stage.
   *
   * @param companyId company
   * @param stage stage
   * @return count
   */
  long countByCompanyIdAndStage(Long companyId, String stage);

  /**
   * Billings of a company awaiting the insurer past their due date.
   *
   * @param companyId company
   * @param stage awaiting stage
   * @param date today
   * @return count
   */
  long countByCompanyIdAndStageAndSlaDueBefore(Long companyId, String stage, LocalDate date);
}
