package com.iortatechnxt.brokerverse.eb.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Roster versions. */
public interface EbRosterVersionRepository extends JpaRepository<EbRosterVersion, Long> {

  /**
   * Versions of a programme.
   *
   * @param programmeId programme
   * @return versions, latest year and version first
   */
  List<EbRosterVersion> findByProgrammeIdOrderByPolicyYearDescVersionNoDesc(Long programmeId);

  /**
   * The version of a programme and year in a status.
   *
   * @param programmeId programme
   * @param policyYear policy year
   * @param status status
   * @return version
   */
  Optional<EbRosterVersion> findFirstByProgrammeIdAndPolicyYearAndStatusOrderByVersionNoDesc(
      Long programmeId, int policyYear, EbRosterVersion.Status status);

  /**
   * The latest version of a programme and year.
   *
   * @param programmeId programme
   * @param policyYear policy year
   * @return version
   */
  Optional<EbRosterVersion> findFirstByProgrammeIdAndPolicyYearOrderByVersionNoDesc(
      Long programmeId, int policyYear);

  /**
   * The version loaded by an upload, locked (rows of one upload commit one by one).
   *
   * @param sourceRef upload number
   * @return version
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EbRosterVersion> findBySourceRef(String sourceRef);

  /**
   * Versions of a company in a status (EB Home).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, EbRosterVersion.Status status);
}
