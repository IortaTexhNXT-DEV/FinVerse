package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PickupStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Check pick-up requests (CSHID.009). */
public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {

  /**
   * Requests of a company in a status, filtered by pick-up date.
   *
   * @param companyId company
   * @param status status
   * @param from pick-up date from
   * @param to pick-up date to
   * @param pageable page
   * @return requests by pick-up date
   */
  @Query(
      "select p from PickupRequest p where p.companyId = :companyId and p.status = :status"
          + " and p.pickupDate >= :from"
          + " and p.pickupDate <= :to"
          + " order by p.pickupDate, p.id")
  Page<PickupRequest> search(
      @Param("companyId") Long companyId,
      @Param("status") PickupStatus status,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);

  /**
   * Requests by id.
   *
   * @param ids ids
   * @return requests
   */
  List<PickupRequest> findByIdInOrderByIdAsc(Collection<Long> ids);

  /**
   * Whether a Collection reference is known.
   *
   * @param companyId company
   * @param collectionRef reference
   * @return true when present
   */
  boolean existsByCompanyIdAndCollectionRef(Long companyId, String collectionRef);

  /**
   * Count of queued requests due by a date.
   *
   * @param companyId company
   * @param status FOR_PICKUP
   * @param date date
   * @return count
   */
  long countByCompanyIdAndStatusAndPickupDateLessThanEqual(
      Long companyId, PickupStatus status, LocalDate date);
}
