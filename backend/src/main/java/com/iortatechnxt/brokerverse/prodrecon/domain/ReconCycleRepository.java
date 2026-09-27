package com.iortatechnxt.brokerverse.prodrecon.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reconciliation cycles. */
public interface ReconCycleRepository extends JpaRepository<ReconCycle, Long> {

  /**
   * The open cycle of an insurer and month.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param month first day of the month
   * @return cycle
   */
  Optional<ReconCycle> findByCompanyIdAndInsurerCodeAndProductionMonthAndClosedFalse(
      Long companyId, String insurerCode, LocalDate month);

  /**
   * Open cycles of an insurer and month in any company (upload without a company context).
   *
   * @param insurerCode insurer
   * @param month first day of the month
   * @return cycles
   */
  List<ReconCycle> findByInsurerCodeAndProductionMonthAndClosedFalse(
      String insurerCode, LocalDate month);

  /**
   * Cycles of a company, newest month first (cycles board, PRCID.013).
   *
   * @param companyId company
   * @param insurer insurer, null for all
   * @param month production month, null for all
   * @param stage stage, null for all
   * @param pageable page
   * @return cycles
   */
  @Query(
      """
      select c from ReconCycle c where c.companyId = :companyId
        and (:insurer is null or c.insurerCode = :insurer)
        and (cast(:month as LocalDate) is null or c.productionMonth = :month)
        and (:stage is null or c.stage = :stage)
      order by c.productionMonth desc, c.insurerCode, c.id desc
      """)
  Page<ReconCycle> search(
      @Param("companyId") Long companyId,
      @Param("insurer") String insurer,
      @Param("month") LocalDate month,
      @Param("stage") String stage,
      Pageable pageable);

  /**
   * Cycles of a company in a range of production months (reports).
   *
   * @param companyId company
   * @param from first month
   * @param to last day
   * @return cycles by month and insurer
   */
  List<ReconCycle> findByCompanyIdAndProductionMonthBetweenOrderByProductionMonthAscInsurerCodeAsc(
      Long companyId, LocalDate from, LocalDate to);

  /**
   * Open cycles of an insurer (automatch on booking, PRCID.024).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @return cycles
   */
  List<ReconCycle> findByCompanyIdAndInsurerCodeAndClosedFalseOrderByIdAsc(
      Long companyId, String insurerCode);

  /**
   * Open cycles in some stages (RECON_AUTOMATCH job).
   *
   * @param stages stages
   * @return cycles
   */
  List<ReconCycle> findByClosedFalseAndStageInOrderByIdAsc(List<String> stages);

  /**
   * Open cycles of a company in a stage (Operations home).
   *
   * @param companyId company
   * @param stage stage
   * @return count
   */
  long countByCompanyIdAndStageAndClosedFalse(Long companyId, String stage);
}
