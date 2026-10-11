package com.iortatechnxt.brokerverse.csf.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Contact changes. */
public interface CsfContactChangeRepository
    extends JpaRepository<CsfContactChange, Long>, JpaSpecificationExecutor<CsfContactChange> {

  /**
   * Changes of a client, newest first, with their field rows.
   *
   * @param clientId client
   * @return changes
   */
  @EntityGraph(attributePaths = "fields")
  List<CsfContactChange> findByClientIdOrderByAppliedAtDescIdDesc(Long clientId);

  /**
   * Changes by id with their field rows.
   *
   * @param ids ids
   * @return changes
   */
  @EntityGraph(attributePaths = "fields")
  List<CsfContactChange> findByIdIn(List<Long> ids);

  /**
   * Changes of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return changes
   */
  Page<CsfContactChange> findByCompanyIdOrderByAppliedAtDescIdDesc(
      Long companyId, Pageable pageable);
}
