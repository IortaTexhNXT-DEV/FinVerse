package com.iortatechnxt.finverse.receivables.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence for {@link Receipt}. */
public interface ReceiptRepository
    extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {

  /**
   * Loads a receipt with its allocations.
   *
   * @param id id
   * @return receipt
   */
  @EntityGraph(attributePaths = "allocations", type = EntityGraph.EntityGraphType.LOAD)
  Optional<Receipt> findWithAllocationsById(Long id);

  /**
   * Lists approved receipts of a bank account in a deposit status.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param status receipt status
   * @param depositStatus deposit status
   * @return receipts oldest first
   */
  List<Receipt>
      findByCompanyIdAndBankAccountCodeAndStatusAndDepositStatusOrderByReceiptDateAscIdAsc(
          Long companyId,
          String bankAccountCode,
          ReceiptStatus status,
          DepositStatus depositStatus);

  /**
   * Lists approved receipts in a deposit status (all banks).
   *
   * @param companyId company
   * @param status receipt status
   * @param depositStatus deposit status
   * @return receipts oldest first
   */
  List<Receipt> findByCompanyIdAndStatusAndDepositStatusOrderByReceiptDateAscIdAsc(
      Long companyId, ReceiptStatus status, DepositStatus depositStatus);

  /**
   * Lists the receipts of a deposit slip.
   *
   * @param slipId slip
   * @return receipts
   */
  List<Receipt> findByDepositSlipIdOrderByIdAsc(Long slipId);

  /**
   * Counts receipts of a company (demo data idempotency).
   *
   * @param companyId company
   * @return count
   */
  long countByCompanyId(Long companyId);
}
