package com.iortatechnxt.brokerverse.eb.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Member changes. */
public interface EbMemberChangeRepository
    extends JpaRepository<EbMemberChange, Long>, JpaSpecificationExecutor<EbMemberChange> {

  /**
   * A member change of a company.
   *
   * @param id change
   * @param companyId company
   * @return change
   */
  Optional<EbMemberChange> findByIdAndCompanyId(Long id, Long companyId);

  /**
   * Member changes of a programme.
   *
   * @param programmeId programme
   * @return changes, latest first
   */
  List<EbMemberChange> findByProgrammeIdOrderByIdDesc(Long programmeId);

  /**
   * Changes of a company in given statuses (EB Home).
   *
   * @param companyId company
   * @param statuses statuses
   * @return count
   */
  long countByCompanyIdAndStatusIn(Long companyId, Collection<EbMemberChange.Status> statuses);
}
