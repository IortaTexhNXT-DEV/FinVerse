package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Special remittance requests. */
public interface SpecialRemittanceRepository
    extends JpaRepository<SpecialRemittance, Long>, JpaSpecificationExecutor<SpecialRemittance> {

  /**
   * The live request of an invoice.
   *
   * @param invoiceNo invoice
   * @param stages live stages
   * @return request
   */
  Optional<SpecialRemittance> findFirstByInvoiceInvoiceNoAndStageIn(
      String invoiceNo, Collection<SpecialStage> stages);

  /**
   * The request of a special batch.
   *
   * @param batchNo batch
   * @return request
   */
  Optional<SpecialRemittance> findByBatchNo(String batchNo);

  /**
   * A request by number.
   *
   * @param requestNo request number
   * @return request
   */
  Optional<SpecialRemittance> findByRequestNo(String requestNo);

  /**
   * Requests of an invoice, newest first.
   *
   * @param invoiceNo invoice
   * @return requests
   */
  List<SpecialRemittance> findByInvoiceInvoiceNoOrderByIdDesc(String invoiceNo);

  /**
   * Requests of a company in some stages.
   *
   * @param companyId company
   * @param stages stages
   * @return count
   */
  long countByCompanyIdAndStageIn(Long companyId, Collection<SpecialStage> stages);

  /**
   * Requests in some stages, oldest first (pending approvals).
   *
   * @param stages stages
   * @return requests
   */
  List<SpecialRemittance> findByStageInOrderByIdAsc(Collection<SpecialStage> stages);
}
