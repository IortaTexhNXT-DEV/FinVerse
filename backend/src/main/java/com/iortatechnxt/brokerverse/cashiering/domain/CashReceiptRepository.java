package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Receipts (CSHID.001/002/010). */
public interface CashReceiptRepository
    extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {

  /**
   * A receipt by number.
   *
   * @param receiptNo AR or OR number
   * @return receipt
   */
  Optional<Receipt> findByReceiptNo(String receiptNo);

  /**
   * The receipt of a source transaction (idempotency of ports and uploads).
   *
   * @param companyId company
   * @param sourceModule module
   * @param sourceRef reference
   * @return receipt
   */
  Optional<Receipt> findByCompanyIdAndSourceModuleAndSourceRef(
      Long companyId, String sourceModule, String sourceRef);

  /**
   * Receipts by id, in id order (batch printing).
   *
   * @param ids ids
   * @return receipts
   */
  List<Receipt> findByIdInOrderByIdAsc(Collection<Long> ids);
}
