package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Hand-offs of the default port adapters. */
public interface OpsHandoffRepository extends JpaRepository<OpsHandoff, Long> {

  /**
   * The hand-off of a source transaction on a port.
   *
   * @param port port
   * @param sourceModule source module
   * @param sourceRef source reference
   * @return hand-off
   */
  Optional<OpsHandoff> findByPortAndSourceModuleAndSourceRef(
      String port, String sourceModule, String sourceRef);

  /**
   * Hand-offs of a company with a status, newest first.
   *
   * @param companyId company
   * @param status status
   * @param pageable page
   * @return hand-offs
   */
  Page<OpsHandoff> findByCompanyIdAndStatusOrderByIdDesc(
      Long companyId, OpsHandoff.Status status, Pageable pageable);

  /**
   * Open hand-offs of a company (Operations home).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, OpsHandoff.Status status);
}
