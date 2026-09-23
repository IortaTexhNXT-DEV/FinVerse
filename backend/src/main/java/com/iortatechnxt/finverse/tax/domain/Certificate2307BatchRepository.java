package com.iortatechnxt.finverse.tax.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** BIR Form 2307 batches. */
public interface Certificate2307BatchRepository extends JpaRepository<Certificate2307Batch, Long> {

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @return batches
   */
  List<Certificate2307Batch> findByCompanyIdOrderByIdDesc(Long companyId);
}
