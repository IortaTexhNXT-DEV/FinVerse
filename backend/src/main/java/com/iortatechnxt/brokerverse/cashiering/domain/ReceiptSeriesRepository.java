package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Receipt series (CSHID.006/015). */
public interface ReceiptSeriesRepository extends JpaRepository<ReceiptSeries, Long> {

  /**
   * Usable series of a branch and kind, locked, oldest first: numbers come from the first one that
   * is not depleted.
   *
   * @param companyId company
   * @param branchId branch
   * @param kind AR or OR
   * @param status ACTIVE
   * @return series
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select s from ReceiptSeries s where s.companyId = :companyId and s.branchId = :branchId"
          + " and s.kind = :kind and s.recordStatus = :status and s.nextNo <= s.toNo order by s.id")
  List<ReceiptSeries> lockUsable(
      @Param("companyId") Long companyId,
      @Param("branchId") Long branchId,
      @Param("kind") ReceiptKind kind,
      @Param("status") RecordStatus status);

  /**
   * Series of a company.
   *
   * @param companyId company
   * @return series by branch, kind and id
   */
  List<ReceiptSeries> findByCompanyIdOrderByBranchIdAscKindAscIdAsc(Long companyId);
}
