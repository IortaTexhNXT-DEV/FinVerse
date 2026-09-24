package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer branches. */
public interface InsurerBranchRepository extends JpaRepository<InsurerBranch, Long> {

  /**
   * Branches of an insurer.
   *
   * @param insurerId insurer profile
   * @return branches
   */
  List<InsurerBranch> findByInsurerIdOrderByCodeAsc(Long insurerId);

  /**
   * One branch.
   *
   * @param insurerId insurer profile
   * @param code branch code
   * @return branch
   */
  Optional<InsurerBranch> findByInsurerIdAndCode(Long insurerId, String code);
}
