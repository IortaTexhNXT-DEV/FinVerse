package com.iortatechnxt.brokerverse.acsl.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** ACSL cases (ACSL 2.5.x-2.6.x). */
public interface AcslCaseRepository extends JpaRepository<AcslCase, Long> {

  /**
   * The case opened for a requester's reference (idempotency of the validation port).
   *
   * @param module requesting module
   * @param reference its reference
   * @return case
   */
  Optional<AcslCase> findByRequesterModuleAndRequesterRef(String module, String reference);

  /**
   * Cases board search: stage, type and a text on the case, invoice, AR or subject.
   *
   * @param companyId company
   * @param stage stage or null
   * @param type type or null
   * @param q lower-case like pattern ({@code %} for all)
   * @param pageable page
   * @return cases
   */
  @Query(
      """
      select c from AcslCase c
      where c.companyId = :companyId
        and (:stage is null or c.stage = :stage)
        and (:type is null or c.caseType = :type)
        and (lower(c.caseNo) like :q
             or lower(c.subject) like :q
             or lower(coalesce(c.account.invoiceNo, '')) like :q
             or lower(coalesce(c.account.rootInvoiceNo, '')) like :q
             or lower(coalesce(c.account.arNo, '')) like :q)
      """)
  Page<AcslCase> search(
      @Param("companyId") Long companyId,
      @Param("stage") CaseStage stage,
      @Param("type") CaseType type,
      @Param("q") String q,
      Pageable pageable);

  /**
   * Cases per stage.
   *
   * @param companyId company
   * @return stage and count
   */
  @Query("select c.stage, count(c) from AcslCase c where c.companyId = :companyId group by c.stage")
  List<Object[]> countByStage(@Param("companyId") Long companyId);

  /**
   * Cases of an invoice family.
   *
   * @param rootInvoiceNo root invoice
   * @return cases, newest first
   */
  List<AcslCase> findByAccountRootInvoiceNoOrderByIdDesc(String rootInvoiceNo);

  /**
   * A case by number.
   *
   * @param caseNo case number
   * @return case
   */
  Optional<AcslCase> findByCaseNo(String caseNo);
}
