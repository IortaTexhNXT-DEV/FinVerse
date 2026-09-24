package com.iortatechnxt.brokerverse.quotation.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Quotation versions. */
public interface QuotationVersionRepository extends JpaRepository<QuotationVersion, Long> {

  /**
   * One version of a quotation.
   *
   * @param quotationId quotation
   * @param versionNo version number
   * @return version
   */
  Optional<QuotationVersion> findByQuotationIdAndVersionNo(Long quotationId, int versionNo);

  /**
   * Every version of a quotation, oldest first.
   *
   * @param quotationId quotation
   * @return versions
   */
  List<QuotationVersion> findByQuotationIdOrderByVersionNo(Long quotationId);
}
