package com.iortatechnxt.brokerverse.opsledger.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Operations invoices. */
public interface OpsInvoiceRepository
    extends JpaRepository<OpsInvoice, Long>, JpaSpecificationExecutor<OpsInvoice> {

  /**
   * An invoice with its components and shares.
   *
   * @param invoiceNo invoice number
   * @return invoice
   */
  @EntityGraph(
      type = EntityGraph.EntityGraphType.LOAD,
      attributePaths = {"components", "shares"})
  Optional<OpsInvoice> findByInvoiceNo(String invoiceNo);

  /**
   * An invoice locked for update (movements, flags and locks change it).
   *
   * @param invoiceNo invoice number
   * @return invoice
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from OpsInvoice i where i.invoiceNo = :invoiceNo")
  Optional<OpsInvoice> lockByInvoiceNo(@Param("invoiceNo") String invoiceNo);

  /**
   * Whether an invoice is in the ledger.
   *
   * @param invoiceNo invoice number
   * @return true when present
   */
  boolean existsByInvoiceNo(String invoiceNo);

  /**
   * Invoices of an account, oldest first.
   *
   * @param arn Account Reference Number
   * @return invoices
   */
  List<OpsInvoice> findByArnOrderByPolicyYearAscIdAsc(String arn);

  /**
   * Invoice numbers already in the ledger among some.
   *
   * @param invoiceNos invoice numbers
   * @return those present
   */
  @Query("select i.invoiceNo from OpsInvoice i where i.invoiceNo in :invoiceNos")
  List<String> findExisting(@Param("invoiceNos") List<String> invoiceNos);

  /**
   * Invoices of a company in some payment statuses (Operations home).
   *
   * @param companyId company
   * @param statuses payment statuses
   * @return count
   */
  long countByCompanyIdAndPaymentStatusIn(Long companyId, List<PaymentStatus> statuses);

  /**
   * Invoices of a company with a payment and a remittance status (Operations home).
   *
   * @param companyId company
   * @param payment payment status
   * @param remittance remittance status
   * @return count
   */
  long countByCompanyIdAndPaymentStatusAndRemittanceStatus(
      Long companyId, PaymentStatus payment, RemittanceStatus remittance);

  /**
   * Invoices on hold.
   *
   * @param companyId company
   * @return count
   */
  long countByCompanyIdAndHoldFlagTrue(Long companyId);

  /**
   * Invoices with a pending negative adjustment.
   *
   * @param companyId company
   * @return count
   */
  long countByCompanyIdAndPendingNegAdjTrue(Long companyId);

  /**
   * Locked invoices.
   *
   * @param companyId company
   * @return count
   */
  long countByCompanyIdAndLockOwnerIsNotNull(Long companyId);

  /**
   * Invoices booked in a period (production).
   *
   * @param companyId company
   * @param from first day
   * @param to last day
   * @return count
   */
  long countByCompanyIdAndClassificationBookingDateBetween(
      Long companyId, LocalDate from, LocalDate to);

  /**
   * Direct payment invoices whose commission is still outstanding (commission receivables).
   *
   * @param companyId company
   * @return count
   */
  @Query(
      "select count(i) from OpsInvoice i join i.components c where i.companyId = :companyId"
          + " and i.dpFlag = true and c.component ="
          + " com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent.COMMISSION"
          + " and c.balance > 0")
  long countDirectPaymentCommissionOutstanding(@Param("companyId") Long companyId);
}
