package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Dispositions of unapplied payments (CSHID.024). */
public interface DispositionRepository extends JpaRepository<Disposition, Long> {

  /**
   * Dispositions of an item, oldest first.
   *
   * @param unappliedId item
   * @return dispositions
   */
  List<Disposition> findByUnappliedIdOrderByIdAsc(Long unappliedId);

  /**
   * The current disposition of an item (one in the given statuses).
   *
   * @param unappliedId item
   * @param statuses statuses
   * @return latest disposition
   */
  Optional<Disposition> findFirstByUnappliedIdAndStatusInOrderByIdDesc(
      Long unappliedId, Collection<DispositionStatus> statuses);

  /**
   * The disposition of a refund request.
   *
   * @param requestNo Disbursement request
   * @return disposition
   */
  Optional<Disposition> findByDisbursementRequestNo(String requestNo);
}
