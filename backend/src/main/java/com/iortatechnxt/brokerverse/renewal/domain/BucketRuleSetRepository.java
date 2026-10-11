package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bucket rule sets. */
public interface BucketRuleSetRepository extends JpaRepository<BucketRuleSet, Long> {

  /**
   * Versions of a company, newest first.
   *
   * @param companyId company
   * @return versions
   */
  List<BucketRuleSet> findByCompanyIdOrderByVersionNoDesc(Long companyId);

  /**
   * The version in a status.
   *
   * @param companyId company
   * @param status status
   * @return versions
   */
  List<BucketRuleSet> findByCompanyIdAndStatus(Long companyId, RuleSetStatus status);

  /**
   * Versions in a status of every company.
   *
   * @param status status
   * @return versions
   */
  List<BucketRuleSet> findByStatus(RuleSetStatus status);
}
