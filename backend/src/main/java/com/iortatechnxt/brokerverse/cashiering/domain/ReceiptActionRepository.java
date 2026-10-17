package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Receipt cancellations and reinstatements (CSHID.001-005). */
public interface ReceiptActionRepository extends JpaRepository<ReceiptAction, Long> {

  /**
   * Actions of a receipt, oldest first.
   *
   * @param receiptId receipt
   * @return actions
   */
  List<ReceiptAction> findByReceiptIdOrderByIdAsc(Long receiptId);

  /**
   * Whether a receipt has an action in one of the stages.
   *
   * @param receiptId receipt
   * @param stages stages
   * @return true when one exists
   */
  boolean existsByReceiptIdAndStageIn(Long receiptId, List<String> stages);

  /**
   * Actions of a company in some stages, newest first.
   *
   * @param companyId company
   * @param stages stages
   * @param pageable page
   * @return actions
   */
  Page<ReceiptAction> findByCompanyIdAndStageInOrderByIdDesc(
      Long companyId, List<String> stages, Pageable pageable);

  /**
   * Count of actions in a stage.
   *
   * @param companyId company
   * @param stage stage
   * @return count
   */
  long countByCompanyIdAndStage(Long companyId, String stage);

  /**
   * Actions in a stage across companies (approval inbox).
   *
   * @param stage stage
   * @return actions, oldest first
   */
  List<ReceiptAction> findByStageOrderByIdAsc(String stage);
}
