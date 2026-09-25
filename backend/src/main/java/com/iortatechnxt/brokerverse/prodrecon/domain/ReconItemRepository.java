package com.iortatechnxt.brokerverse.prodrecon.domain;

import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UnbookedStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reconciliation items. */
public interface ReconItemRepository
    extends JpaRepository<ReconItem, Long>, JpaSpecificationExecutor<ReconItem> {

  /**
   * Every item of a cycle in creation order.
   *
   * @param cycleId cycle
   * @return items
   */
  List<ReconItem> findByCycleIdOrderByIdAsc(Long cycleId);

  /**
   * The item of a booked invoice in a cycle.
   *
   * @param cycleId cycle
   * @param invoiceNo invoice
   * @return item
   */
  Optional<ReconItem> findByCycleIdAndInvoiceNo(Long cycleId, String invoiceNo);

  /**
   * Items of an invoice in every cycle (invoice 360).
   *
   * @param invoiceNo invoice
   * @return items, newest first
   */
  List<ReconItem> findByInvoiceNoOrderByIdDesc(String invoiceNo);

  /**
   * Item counts per cycle and status.
   *
   * @param cycleIds cycles
   * @return rows of cycle id, status and count
   */
  @Query(
      "select i.cycleId, i.status, count(i) from ReconItem i where i.cycleId in :cycleIds"
          + " group by i.cycleId, i.status")
  List<Object[]> countByStatus(@Param("cycleIds") Collection<Long> cycleIds);

  /**
   * Insurer production without a booked invoice in the open cycles of an insurer whose reference or
   * policy number is one of a new invoice (automatch on booking, PRCID.024).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param invoiceNo new invoice number
   * @param policyNo its policy number, may be null
   * @return items
   */
  @Query(
      """
      select i from ReconItem i, ReconCycle c where c.id = i.cycleId and c.closed = false
        and c.companyId = :companyId and c.insurerCode = :insurer and i.invoiceNo is null
        and (i.insurer.referenceNo = :invoiceNo or i.insurer.policyNo = :policyNo)
      """)
  List<ReconItem> waitingFor(
      @Param("companyId") Long companyId,
      @Param("insurer") String insurerCode,
      @Param("invoiceNo") String invoiceNo,
      @Param("policyNo") String policyNo);

  /**
   * The unbooked repository (PRCID.019/033): insurer production without a booked invoice.
   *
   * @param companyId company
   * @param insurer insurer, null for all
   * @param status resolution, null for all
   * @param text part of the policy, reference or assured name (lower case), null for all
   * @param pageable page
   * @return items
   */
  @Query(
      """
      select i from ReconItem i, ReconCycle c where c.id = i.cycleId and c.companyId = :companyId
        and i.unbookedStatus is not null
        and (:insurer is null or c.insurerCode = :insurer)
        and (:status is null or i.unbookedStatus = :status)
        and (cast(:text as string) is null
             or lower(coalesce(i.insurer.policyNo, '')) like concat('%', cast(:text as string), '%')
             or lower(coalesce(i.insurer.referenceNo, '')) like concat('%', cast(:text as string), '%')
             or lower(coalesce(i.insurer.assuredName, '')) like concat('%', cast(:text as string), '%'))
      order by i.id desc
      """)
  Page<ReconItem> unbooked(
      @Param("companyId") Long companyId,
      @Param("insurer") String insurer,
      @Param("status") UnbookedStatus status,
      @Param("text") String text,
      Pageable pageable);

  /**
   * Open unbooked items of a company (Operations home).
   *
   * @param companyId company
   * @param statuses resolutions
   * @return count
   */
  @Query(
      "select count(i) from ReconItem i, ReconCycle c where c.id = i.cycleId"
          + " and c.companyId = :companyId and i.unbookedStatus in :statuses")
  long countUnbooked(
      @Param("companyId") Long companyId, @Param("statuses") Collection<UnbookedStatus> statuses);
}
