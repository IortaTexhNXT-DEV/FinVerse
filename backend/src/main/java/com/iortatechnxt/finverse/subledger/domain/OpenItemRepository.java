package com.iortatechnxt.finverse.subledger.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link OpenItem}. */
public interface OpenItemRepository extends JpaRepository<OpenItem, Long> {

  /**
   * Lists items of a party, oldest first.
   *
   * @param companyId company
   * @param partyId party
   * @return items
   */
  List<OpenItem> findByCompanyIdAndPartyIdOrderByDocumentDateAscIdAsc(Long companyId, Long partyId);

  /**
   * Lists items not fully settled, in given statuses, up to a document date.
   *
   * @param companyId company
   * @param statuses statuses
   * @param asOf document date bound
   * @return items
   */
  @Query(
      """
      select i from OpenItem i
      where i.companyId = :companyId and i.status in :statuses and i.documentDate <= :asOf
      order by i.partyCode, i.dueDate
      """)
  List<OpenItem> findOutstanding(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<OpenItemStatus> statuses,
      @Param("asOf") LocalDate asOf);

  /**
   * Checks whether an item exists for a source record (idempotency).
   *
   * @param companyId company
   * @param sourceModule module
   * @param sourceReference key
   * @return true when present
   */
  boolean existsByCompanyIdAndSourceModuleAndSourceReference(
      Long companyId, String sourceModule, String sourceReference);
}
