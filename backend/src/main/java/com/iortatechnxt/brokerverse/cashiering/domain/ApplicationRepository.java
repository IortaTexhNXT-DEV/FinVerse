package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Applications of payments to invoices (CSHID.020/022). */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

  /**
   * Applications of a receipt, oldest first.
   *
   * @param receiptId receipt
   * @return applications
   */
  List<Application> findByReceiptIdOrderByIdAsc(Long receiptId);

  /**
   * Applications of an invoice, oldest first.
   *
   * @param invoiceNo invoice
   * @return applications
   */
  List<Application> findByInvoiceNoOrderByIdAsc(String invoiceNo);

  /**
   * Active applications of an invoice, oldest first.
   *
   * @param invoiceNo invoice
   * @param status ACTIVE
   * @return applications
   */
  List<Application> findByInvoiceNoAndStatusOrderByIdAsc(String invoiceNo, String status);

  /**
   * Applications made from an unapplied item.
   *
   * @param unappliedId unapplied item
   * @return applications
   */
  List<Application> findByUnappliedIdOrderByIdAsc(Long unappliedId);

  /**
   * Active applications of accounts (payment confirmations to placement).
   *
   * @param companyId company
   * @param arns accounts
   * @return applications
   */
  @Query(
      "select a from Application a where a.companyId = :companyId and a.arn in :arns"
          + " and a.status = 'ACTIVE' order by a.id")
  List<Application> activeForArns(
      @Param("companyId") Long companyId, @Param("arns") Collection<String> arns);
}
