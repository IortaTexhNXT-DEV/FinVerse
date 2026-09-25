package com.iortatechnxt.brokerverse.screening.config.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Screening configuration versions (SNSRP-101-109). */
public interface ConfigVersionRepository extends JpaRepository<ConfigVersion, Long> {

  /**
   * The approved version with the latest effective date on or before a date (the version in force).
   *
   * @param companyId company
   * @param type type
   * @param scope scope ('' when none)
   * @param statuses ACTIVE and SUPERSEDED
   * @param asOf date
   * @return the version in force
   */
  Optional<ConfigVersion>
      findFirstByCompanyIdAndConfigTypeAndScopeAndStatusInAndEffectiveFromLessThanEqualOrderByEffectiveFromDescVersionNoDesc(
          Long companyId,
          ConfigType type,
          String scope,
          Collection<ConfigStatus> statuses,
          LocalDate asOf);

  /**
   * The newest version of a type and scope in one of the statuses.
   *
   * @param companyId company
   * @param type type
   * @param scope scope ('' when none)
   * @param statuses statuses
   * @return the version with the highest number
   */
  Optional<ConfigVersion> findFirstByCompanyIdAndConfigTypeAndScopeAndStatusInOrderByVersionNoDesc(
      Long companyId, ConfigType type, String scope, Collection<ConfigStatus> statuses);

  /**
   * The versions of a type and scope in one status, latest effective date first.
   *
   * @param companyId company
   * @param type type
   * @param scope scope ('' when none)
   * @param status status
   * @return versions
   */
  List<ConfigVersion> findByCompanyIdAndConfigTypeAndScopeAndStatusOrderByEffectiveFromDesc(
      Long companyId, ConfigType type, String scope, ConfigStatus status);

  /**
   * Every version of a type (all scopes), newest first.
   *
   * @param companyId company
   * @param type type
   * @return versions
   */
  List<ConfigVersion> findByCompanyIdAndConfigTypeOrderByScopeAscVersionNoDesc(
      Long companyId, ConfigType type);

  /**
   * Versions in a status (the approval inbox reads PENDING).
   *
   * @param status status
   * @return versions, oldest submission first
   */
  List<ConfigVersion> findByStatusOrderBySubmittedAtAsc(ConfigStatus status);

  /**
   * Distinct company, type and scope combinations that have an ACTIVE version (supersession sweep).
   *
   * @return versions ACTIVE today
   */
  List<ConfigVersion> findByStatus(ConfigStatus status);

  /**
   * The highest version number of a type and scope.
   *
   * @param companyId company
   * @param type type
   * @param scope scope ('' when none)
   * @return the number, 0 when none
   */
  @Query(
      "select coalesce(max(v.versionNo), 0) from ConfigVersion v where v.companyId = :companyId"
          + " and v.configType = :type and v.scope = :scope")
  int maxVersionNo(
      @Param("companyId") Long companyId,
      @Param("type") ConfigType type,
      @Param("scope") String scope);
}
