package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PdcStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** PDC warehouse (CSHID.008 item 4). */
public interface PdcItemRepository extends JpaRepository<PdcItem, Long> {

  /**
   * Checks of a company, filtered, by maturity.
   *
   * @param companyId company
   * @param status status, null for all
   * @param from maturity from
   * @param to maturity to
   * @param pageable page
   * @return checks
   */
  @Query(
      "select p from PdcItem p where p.companyId = :companyId"
          + " and (:status is null or p.status = :status)"
          + " and p.maturityDate >= :from"
          + " and p.maturityDate <= :to"
          + " order by p.maturityDate, p.id")
  Page<PdcItem> search(
      @Param("companyId") Long companyId,
      @Param("status") PdcStatus status,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);

  /**
   * Warehoused checks due on or before a date (maturity job).
   *
   * @param status WAREHOUSED
   * @param date business date
   * @return checks
   */
  List<PdcItem> findByStatusAndMaturityDateLessThanEqualOrderByIdAsc(
      PdcStatus status, LocalDate date);

  /**
   * Whether a check is already in the warehouse.
   *
   * @param companyId company
   * @param bankCode bank
   * @param checkNo check number
   * @return true when present
   */
  boolean existsByCompanyIdAndBankCodeAndCheckNo(Long companyId, String bankCode, String checkNo);

  /**
   * Count by status.
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, PdcStatus status);
}
