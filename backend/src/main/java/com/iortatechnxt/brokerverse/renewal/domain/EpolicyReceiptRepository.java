package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** E-policy receipts. */
public interface EpolicyReceiptRepository extends JpaRepository<EpolicyReceipt, Long> {

  /**
   * The receipts of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return receipts
   */
  Page<EpolicyReceipt> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * A receipt by its number.
   *
   * @param companyId company
   * @param receiptNo number
   * @return receipt
   */
  Optional<EpolicyReceipt> findByCompanyIdAndReceiptNo(Long companyId, String receiptNo);
}
