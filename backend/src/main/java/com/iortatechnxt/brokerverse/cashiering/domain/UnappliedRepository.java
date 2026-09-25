package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Unapplied payments (CSHID.024/025). */
public interface UnappliedRepository extends JpaRepository<Unapplied, Long> {

  /**
   * An item by reference.
   *
   * @param reference UNP- reference
   * @return item
   */
  Optional<Unapplied> findByReference(String reference);

  /**
   * The item of a source transaction (idempotency of the port).
   *
   * @param companyId company
   * @param sourceModule module
   * @param sourceRef reference
   * @return item
   */
  Optional<Unapplied> findByCompanyIdAndSourceModuleAndSourceRef(
      Long companyId, String sourceModule, String sourceRef);

  /**
   * Items of a company in some stages, filtered by text, newest first.
   *
   * @param companyId company
   * @param stages stages of the tab
   * @param text reference, client, payor or invoice fragment (lower case), null for all
   * @param pageable page
   * @return items
   */
  @Query(
      "select u from Unapplied u where u.companyId = :companyId and u.stage in :stages"
          + " and (:text is null or lower(u.reference) like :text or lower(u.clientCode) like :text"
          + " or lower(u.payorName) like :text or lower(u.invoiceNo) like :text) order by u.id desc")
  Page<Unapplied> search(
      @Param("companyId") Long companyId,
      @Param("stages") Collection<String> stages,
      @Param("text") String text,
      Pageable pageable);

  /**
   * Items in a stage with a positive balance up to an amount (minimal excess sweep).
   *
   * @param stage UNAPPLIED
   * @param max maximum balance
   * @return items
   */
  @Query(
      "select u from Unapplied u where u.stage = :stage"
          + " and u.balance > 0 and u.balance <= :max order by u.id")
  List<Unapplied> smallBalances(@Param("stage") String stage, @Param("max") BigDecimal max);

  /**
   * Items of some origins in a stage (automatch).
   *
   * @param stage UNAPPLIED
   * @param origins origins
   * @return items, oldest first
   */
  List<Unapplied> findByStageAndOriginInOrderByIdAsc(
      String stage, Collection<UnappliedOrigin> origins);

  /**
   * Items of a receipt.
   *
   * @param receiptId receipt
   * @return items
   */
  List<Unapplied> findByReceiptIdOrderByIdAsc(Long receiptId);

  /**
   * Count per stage.
   *
   * @param companyId company
   * @param stage stage
   * @return count
   */
  long countByCompanyIdAndStage(Long companyId, String stage);

  /**
   * Items in a stage across companies (approval inbox).
   *
   * @param stage stage
   * @param pageable limit
   * @return items, oldest first
   */
  List<Unapplied> findByStageOrderByIdAsc(String stage, Pageable pageable);
}
