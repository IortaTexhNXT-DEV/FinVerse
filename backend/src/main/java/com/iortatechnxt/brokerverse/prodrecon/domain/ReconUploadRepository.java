package com.iortatechnxt.brokerverse.prodrecon.domain;

import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UploadStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Insurer upload attempts (PRCID.031/032). */
public interface ReconUploadRepository extends JpaRepository<ReconUpload, Long> {

  /**
   * An earlier upload of the same file that was taken in (duplicate block, PRCID.010).
   *
   * @param sha256 checksum
   * @param statuses statuses of taken-in uploads
   * @return the first such upload
   */
  Optional<ReconUpload> findFirstBySha256AndStatusInOrderByIdAsc(
      String sha256, List<UploadStatus> statuses);

  /**
   * Attempts made by a flow-in run.
   *
   * @param runNo run number
   * @return attempts
   */
  List<ReconUpload> findByRunNoOrderByIdAsc(String runNo);

  /**
   * Attempts so far for an insurer and month.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param month first day of the month
   * @return count
   */
  long countByCompanyIdAndInsurerCodeAndProductionMonth(
      Long companyId, String insurerCode, LocalDate month);

  /**
   * Attempts of a cycle, newest first.
   *
   * @param cycleId cycle
   * @return attempts
   */
  List<ReconUpload> findByCycleIdOrderByIdDesc(Long cycleId);

  /**
   * Upload history of a company, newest first.
   *
   * @param companyId company
   * @param insurer insurer, null for all
   * @param pageable page
   * @return attempts
   */
  @Query(
      """
      select u from ReconUpload u where u.companyId = :companyId
        and (:insurer is null or u.insurerCode = :insurer)
      order by u.id desc
      """)
  Page<ReconUpload> search(
      @Param("companyId") Long companyId, @Param("insurer") String insurer, Pageable pageable);
}
