package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Pre-booked payments (CSHID.020). */
public interface PrebookedRepository extends JpaRepository<Prebooked, Long> {

  /**
   * Items of a company in a status, oldest first.
   *
   * @param companyId company
   * @param status status
   * @param pageable page
   * @return items
   */
  Page<Prebooked> findByCompanyIdAndStatusOrderByIdAsc(
      Long companyId, String status, Pageable pageable);

  /**
   * Open items of an account.
   *
   * @param arn account
   * @param status OPEN
   * @return items
   */
  List<Prebooked> findByArnAndStatusOrderByIdAsc(String arn, String status);

  /**
   * Items of a receipt in a status.
   *
   * @param receiptId AR
   * @param status status
   * @return items
   */
  List<Prebooked> findByReceiptIdAndStatus(Long receiptId, String status);

  /**
   * Items of a status across companies (jobs).
   *
   * @param status status
   * @return items
   */
  List<Prebooked> findByStatusOrderByIdAsc(String status);

  /**
   * Items of accounts in a status (payment confirmations to placement).
   *
   * @param companyId company
   * @param arns accounts
   * @param status status
   * @return items
   */
  List<Prebooked> findByCompanyIdAndArnInAndStatus(
      Long companyId, Collection<String> arns, String status);

  /**
   * Count of items in a status.
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, String status);
}
