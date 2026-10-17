package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Hold requests. */
public interface HoldRequestRepository
    extends JpaRepository<HoldRequest, Long>, JpaSpecificationExecutor<HoldRequest> {

  /**
   * The live request of an invoice (one at a time, MKTID.005).
   *
   * @param invoiceNo invoice
   * @param stages live stages
   * @return request
   */
  Optional<HoldRequest> findFirstByInvoiceInvoiceNoAndStageIn(
      String invoiceNo, Collection<HoldStage> stages);

  /**
   * Requests of an invoice, newest first.
   *
   * @param invoiceNo invoice
   * @return requests
   */
  List<HoldRequest> findByInvoiceInvoiceNoOrderByIdDesc(String invoiceNo);

  /**
   * Active holds whose date is before a day (expiry, RMTID.021).
   *
   * @param stage stage
   * @param day day
   * @return requests
   */
  List<HoldRequest> findByStageAndHoldUntilBeforeOrderByIdAsc(HoldStage stage, LocalDate day);

  /**
   * Active holds reaching their date on or before a day and not yet notified (HOLD_EXPIRING).
   *
   * @param stage stage
   * @param day day
   * @return requests
   */
  List<HoldRequest> findByStageAndHoldUntilLessThanEqualAndExpiryNotifiedFalseOrderByIdAsc(
      HoldStage stage, LocalDate day);

  /**
   * Requests of a company in some stages.
   *
   * @param companyId company
   * @param stages stages
   * @return count
   */
  long countByCompanyIdAndStageIn(Long companyId, Collection<HoldStage> stages);

  /**
   * Requests in some stages, oldest first (pending approvals).
   *
   * @param stages stages
   * @return requests
   */
  List<HoldRequest> findByStageInOrderByIdAsc(Collection<HoldStage> stages);
}
