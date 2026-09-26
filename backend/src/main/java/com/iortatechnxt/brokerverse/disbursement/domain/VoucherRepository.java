package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PostingStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Disbursement vouchers (DIS 2.7-2.21). */
public interface VoucherRepository
    extends JpaRepository<Voucher, Long>, JpaSpecificationExecutor<Voucher> {

  /**
   * A voucher with its proforma lines.
   *
   * @param id id
   * @return voucher
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "lines")
  Optional<Voucher> findWithLinesById(Long id);

  /**
   * A voucher by number.
   *
   * @param dvNo DV number
   * @return voucher
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "lines")
  Optional<Voucher> findByDvNo(String dvNo);

  /**
   * The voucher of a payment request.
   *
   * @param requestId request
   * @return voucher
   */
  Optional<Voucher> findByRequestId(Long requestId);

  /**
   * Vouchers in some stages, oldest first (pending approvals).
   *
   * @param stages stages
   * @return vouchers
   */
  List<Voucher> findByStageInOrderByIdAsc(Collection<VoucherStage> stages);

  /**
   * Approved vouchers not yet in an end-of-day run, approved before a time (DIS 2.16.0).
   *
   * @param companyId company
   * @param stage APPROVED
   * @param before end of the business date
   * @return vouchers, oldest first
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "lines")
  List<Voucher> findByCompanyIdAndStageAndEodRunIdIsNullAndApprovedAtBeforeOrderByIdAsc(
      Long companyId, VoucherStage stage, Instant before);

  /**
   * The vouchers of an end-of-day run.
   *
   * @param eodRunId run
   * @return vouchers
   */
  List<Voucher> findByEodRunIdOrderByIdAsc(Long eodRunId);

  /**
   * Vouchers of a company in some stages.
   *
   * @param companyId company
   * @param stages stages
   * @return count
   */
  long countByCompanyIdAndStageIn(Long companyId, Collection<VoucherStage> stages);

  /**
   * Vouchers of a company with some posting statuses (unregularised, DIS 3.27.0).
   *
   * @param companyId company
   * @param statuses posting statuses
   * @return count
   */
  long countByCompanyIdAndPostingStatusIn(Long companyId, Collection<PostingStatus> statuses);
}
