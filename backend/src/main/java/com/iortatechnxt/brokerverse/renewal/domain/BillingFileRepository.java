package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** CLPC billing files. */
public interface BillingFileRepository extends JpaRepository<BillingFile, Long> {

  /**
   * The files of a company, newest first.
   *
   * @param companyId company
   * @return files
   */
  List<BillingFile> findTop200ByCompanyIdOrderByIdDesc(Long companyId);
}
