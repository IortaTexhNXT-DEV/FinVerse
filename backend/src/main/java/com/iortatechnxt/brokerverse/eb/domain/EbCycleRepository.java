package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** EB cycles. */
public interface EbCycleRepository extends JpaRepository<EbCycle, Long> {

  /**
   * A cycle by number.
   *
   * @param cycleNo cycle number
   * @return cycle
   */
  Optional<EbCycle> findByCycleNo(String cycleNo);

  /**
   * The cycles of a programme, latest policy year first.
   *
   * @param programmeId programme
   * @return cycles
   */
  List<EbCycle> findByProgrammeIdOrderByPolicyYearDescIdDesc(Long programmeId);

  /**
   * The open cycle of a programme for a policy year (at most one, V1033).
   *
   * @param programmeId programme
   * @param policyYear policy year
   * @return cycle
   */
  @Query(
      "select c from EbCycle c where c.programmeId = :programmeId and c.policyYear = :policyYear"
          + " and c.closedAt is null")
  Optional<EbCycle> findOpen(
      @Param("programmeId") Long programmeId, @Param("policyYear") int policyYear);

  /**
   * The cycle an account was created for.
   *
   * @param arn Account Reference Number
   * @return cycle
   */
  @Query("select c from EbCycle c join c.accountArns a where a = :arn")
  Optional<EbCycle> findByAccountArn(@Param("arn") String arn);

  /**
   * A cycle of a company.
   *
   * @param id cycle
   * @param companyId company
   * @return cycle
   */
  Optional<EbCycle> findByIdAndCompanyId(Long id, Long companyId);

  /**
   * The open cycles of a programme, latest policy year first.
   *
   * @param programmeId programme
   * @return open cycles
   */
  @Query(
      "select c from EbCycle c where c.programmeId = :programmeId and c.closedAt is null"
          + " order by c.policyYear desc, c.id desc")
  List<EbCycle> findOpenOfProgramme(@Param("programmeId") Long programmeId);

  /**
   * Number of cycles of a company per stage (EB Home tiles).
   *
   * @param companyId company
   * @return rows of stage and count
   */
  @Query(
      "select c.stage, count(c) from EbCycle c where c.companyId = :companyId"
          + " and c.closedAt is null group by c.stage")
  List<Object[]> countOpenByStage(@Param("companyId") Long companyId);
}
