package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PostingStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the Disbursement workbench (DIS 2.4.0-2.4.4, 2.6.2, 2.13.0): payment requests by status
 * and received date, vouchers by stage with search on DV, request, payee and invoice, and the
 * counts of every tab.
 */
@Service
@Transactional(readOnly = true)
public class DisbursementQueryService {

  private static final String COMPANY = "companyId";

  private final IntakeRequestRepository requests;
  private final VoucherRepository vouchers;
  private final PayeeRepository payees;
  private final PayeeRequestRepository payeeRequests;
  private final FundingRequestRepository fundings;

  /**
   * Creates the service.
   *
   * @param requests payment requests
   * @param vouchers vouchers
   * @param payees payees
   * @param payeeRequests payee requests
   * @param fundings funding requests
   */
  public DisbursementQueryService(
      IntakeRequestRepository requests,
      VoucherRepository vouchers,
      PayeeRepository payees,
      PayeeRequestRepository payeeRequests,
      FundingRequestRepository fundings) {
    this.requests = requests;
    this.vouchers = vouchers;
    this.payees = payees;
    this.payeeRequests = payeeRequests;
    this.fundings = fundings;
  }

  /**
   * Payment requests (DIS 2.4.0-2.4.1): statuses, received between two dates, text in the request
   * number, RFP, source reference or payee.
   *
   * @param s filters
   * @param pageable page
   * @return requests
   */
  public Page<IntakeRequest> requests(RequestSearch s, Pageable pageable) {
    Specification<IntakeRequest> spec =
        (root, query, cb) -> {
          List<Predicate> where = new ArrayList<>();
          where.add(cb.equal(root.get(COMPANY), s.companyId()));
          if (s.statuses() != null && !s.statuses().isEmpty()) {
            where.add(root.get("status").in(s.statuses()));
          }
          if (s.from() != null) {
            where.add(
                cb.greaterThanOrEqualTo(
                    root.get("receivedAt"),
                    s.from().atStartOfDay(DisbursementSettings.MANILA).toInstant()));
          }
          if (s.to() != null) {
            where.add(
                cb.lessThan(
                    root.get("receivedAt"),
                    s.to().plusDays(1).atStartOfDay(DisbursementSettings.MANILA).toInstant()));
          }
          text(
              s.text(),
              root,
              cb,
              where,
              "requestNo",
              "rfpNo",
              "sourceRef",
              "payeeCode",
              "payeeName");
          return cb.and(where.toArray(Predicate[]::new));
        };
    return requests.findAll(spec, pageable);
  }

  /**
   * Vouchers (DIS 2.4.4, 2.13.0, 2.18.0): stages, mode, text in the DV, payee or root invoice.
   *
   * @param s filters
   * @param pageable page
   * @return vouchers
   */
  public Page<Voucher> vouchers(VoucherSearch s, Pageable pageable) {
    Specification<Voucher> spec =
        (root, query, cb) -> {
          List<Predicate> where = new ArrayList<>();
          where.add(cb.equal(root.get(COMPANY), s.companyId()));
          if (s.stages() != null && !s.stages().isEmpty()) {
            where.add(root.get("stage").in(s.stages()));
          }
          if (s.type() != null && !s.type().isBlank()) {
            where.add(cb.equal(root.get("disbursementType"), s.type()));
          }
          if (s.unregularized()) {
            where.add(
                root.get("postingStatus")
                    .in(EnumSet.of(PostingStatus.FAILED, PostingStatus.REVERSAL_FAILED)));
          }
          text(s.text(), root, cb, where, "dvNo", "payeeCode", "payeeName", "rootInvoiceNo");
          return cb.and(where.toArray(Predicate[]::new));
        };
    return vouchers.findAll(spec, pageable);
  }

  private static <T> void text(
      String text, Root<T> root, CriteriaBuilder cb, List<Predicate> where, String... fields) {
    if (text == null || text.isBlank()) {
      return;
    }
    String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    List<Predicate> any = new ArrayList<>();
    for (String f : fields) {
      any.add(cb.like(cb.lower(root.get(f)), like));
    }
    where.add(cb.or(any.toArray(Predicate[]::new)));
  }

  /**
   * The counts of the workbench tabs and the waiting masters.
   *
   * @param companyId company
   * @return counts
   */
  public Summary summary(Long companyId) {
    return new Summary(
        requests.countByCompanyIdAndStatusIn(companyId, EnumSet.of(RequestStatus.RECEIVED)),
        requests.countByCompanyIdAndStatusIn(companyId, EnumSet.of(RequestStatus.NO_PAYEE)),
        count(companyId, VoucherStage.IN_PROCESS),
        count(companyId, VoucherStage.FOR_REVIEW),
        count(companyId, VoucherStage.FOR_APPROVAL),
        count(companyId, VoucherStage.APPROVED),
        vouchers.countByCompanyIdAndStageIn(
            companyId, EnumSet.of(VoucherStage.CANCELLED, VoucherStage.REJECTED)),
        vouchers.countByCompanyIdAndPostingStatusIn(
            companyId, EnumSet.of(PostingStatus.FAILED, PostingStatus.REVERSAL_FAILED)),
        payees.countByCompanyIdAndStageIn(
            companyId,
            EnumSet.of(
                PayeeStage.FOR_AUTHORIZATION,
                PayeeStage.FOR_DEACTIVATION,
                PayeeStage.FOR_REACTIVATION)),
        payeeRequests.countByCompanyIdAndStatus(companyId, PayeeRequestStatus.OPEN),
        fundings.countByCompanyIdAndStageIn(
            companyId,
            EnumSet.of(
                FundingStage.FOR_VERIFICATION,
                FundingStage.FOR_APPROVAL_1,
                FundingStage.FOR_APPROVAL_2)));
  }

  private long count(Long companyId, VoucherStage stage) {
    return vouchers.countByCompanyIdAndStageIn(companyId, EnumSet.of(stage));
  }

  /**
   * Request filters.
   *
   * @param companyId company
   * @param statuses statuses, empty for all
   * @param from received from
   * @param to received to
   * @param text search text
   */
  public record RequestSearch(
      Long companyId, List<RequestStatus> statuses, LocalDate from, LocalDate to, String text) {}

  /**
   * Voucher filters.
   *
   * @param companyId company
   * @param stages stages, empty for all
   * @param type disbursement type, null for all
   * @param text search text
   * @param unregularized only vouchers whose posting or reversal failed
   */
  public record VoucherSearch(
      Long companyId, List<VoucherStage> stages, String type, String text, boolean unregularized) {}

  /**
   * Counts of the workbench.
   *
   * @param requests requests received without voucher
   * @param noPayee requests waiting for their payee
   * @param inProcess vouchers in process
   * @param forReview vouchers for review
   * @param forApproval vouchers for approval
   * @param approved approved vouchers
   * @param closed cancelled and rejected vouchers
   * @param unregularized vouchers whose posting or reversal failed
   * @param payeesPending payees waiting for authorisation
   * @param payeeRequests open payee requests
   * @param fundingPending funding requests waiting for verification or approval
   */
  public record Summary(
      long requests,
      long noPayee,
      long inProcess,
      long forReview,
      long forApproval,
      long approved,
      long closed,
      long unregularized,
      long payeesPending,
      long payeeRequests,
      long fundingPending) {}
}
