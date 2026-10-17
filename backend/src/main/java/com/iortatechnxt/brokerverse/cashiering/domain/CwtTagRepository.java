package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** BIR 2307 tags (CSHID.026/027, MKTID.013). */
public interface CwtTagRepository extends JpaRepository<CwtTag, Long> {

  /**
   * A tag by reference.
   *
   * @param reference CWT- reference
   * @return tag
   */
  Optional<CwtTag> findByReference(String reference);

  /**
   * Tags of a company in some stages, newest first.
   *
   * @param companyId company
   * @param stages stages
   * @param pageable page
   * @return tags
   */
  Page<CwtTag> findByCompanyIdAndStageInOrderByIdDesc(
      Long companyId, Collection<String> stages, Pageable pageable);

  /**
   * Tags of a batch.
   *
   * @param batchId batch
   * @return tags
   */
  List<CwtTag> findByBatchIdOrderByIdAsc(Long batchId);

  /**
   * Tags by id.
   *
   * @param ids ids
   * @return tags
   */
  List<CwtTag> findByIdInOrderByIdAsc(Collection<Long> ids);

  /**
   * Live tags of an invoice (one live tag per invoice).
   *
   * @param invoiceNo invoice
   * @param stages stages that count as live
   * @return true when one exists
   */
  boolean existsByInvoiceNoAndStageIn(String invoiceNo, Collection<String> stages);

  /**
   * Count per stage.
   *
   * @param companyId company
   * @param stage stage
   * @return count
   */
  long countByCompanyIdAndStage(Long companyId, String stage);
}
