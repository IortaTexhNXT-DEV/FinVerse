package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Rule set versions. */
public interface SbmRuleSetRepository extends JpaRepository<SbmRuleSet, Long> {

  /**
   * Rule sets of a company, newest version first per code.
   *
   * @param companyId company
   * @return rule sets
   */
  List<SbmRuleSet> findByCompanyIdOrderByStepAscCodeAscVersionNoDesc(Long companyId);

  /**
   * Rule sets in some statuses (approval inbox, processing).
   *
   * @param statuses statuses
   * @return rule sets
   */
  List<SbmRuleSet> findByStatusInOrderByIdAsc(Collection<SbmRuleSetStatus> statuses);

  /**
   * Rule sets of a company and step in a status.
   *
   * @param companyId company
   * @param step step
   * @param status status
   * @return rule sets
   */
  List<SbmRuleSet> findByCompanyIdAndStepAndStatus(
      Long companyId, SbmStep step, SbmRuleSetStatus status);

  /**
   * The versions of a code in some statuses.
   *
   * @param companyId company
   * @param code code
   * @param statuses statuses
   * @return versions
   */
  List<SbmRuleSet> findByCompanyIdAndCodeAndStatusIn(
      Long companyId, String code, Collection<SbmRuleSetStatus> statuses);

  /**
   * The highest version of a code.
   *
   * @param companyId company
   * @param code code
   * @return version
   */
  Optional<SbmRuleSet> findFirstByCompanyIdAndCodeOrderByVersionNoDesc(Long companyId, String code);
}
