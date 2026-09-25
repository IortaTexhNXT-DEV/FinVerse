package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.StatusEditStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.StatusEdit;
import com.iortatechnxt.brokerverse.disbursement.domain.StatusEditRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disbursement items of the universal approval inbox: vouchers for approval ({@code DISB_APPROVE},
 * never to their processor or checker), payees waiting for authorisation ({@code
 * DISB_PAYEE_AUTHORIZE}), funding requests for verification and approval ({@code
 * DISB_FUNDING_VERIFY}, {@code DISB_FUNDING_APPROVE}) and status edits ({@code
 * DISB_STATUS_APPROVE}); makers never see their own items.
 */
@Component
@Transactional(readOnly = true)
public class DisbursementApprovalSource implements PendingApprovalSource {

  private static final String MODULE = "DISBURSEMENT";

  private final VoucherRepository vouchers;
  private final PayeeRepository payees;
  private final FundingRequestRepository fundings;
  private final StatusEditRepository edits;
  private final InstrumentRepository instruments;

  /**
   * Creates the source.
   *
   * @param vouchers vouchers
   * @param payees payees
   * @param fundings funding requests
   * @param edits status edits
   * @param instruments instruments (link of a status edit)
   */
  public DisbursementApprovalSource(
      VoucherRepository vouchers,
      PayeeRepository payees,
      FundingRequestRepository fundings,
      StatusEditRepository edits,
      InstrumentRepository instruments) {
    this.vouchers = vouchers;
    this.payees = payees;
    this.fundings = fundings;
    this.edits = edits;
    this.instruments = instruments;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> items = new ArrayList<>();
    if (viewer.can(DisbursementSettings.APPROVE)) {
      vouchers(viewer, items);
    }
    if (viewer.can(DisbursementSettings.PAYEE_AUTHORIZE)) {
      payees(viewer, items);
    }
    fundings(viewer, items);
    if (viewer.can("DISB_STATUS_APPROVE")) {
      for (StatusEdit e : edits.findByStageOrderByIdAsc(StatusEditStage.REQUESTED)) {
        if (viewer.mayApproveItemOf(e.getCreatedBy())) {
          items.add(
              new PendingApproval(
                  MODULE,
                  "Instrument status edit",
                  "EDIT-" + e.getId(),
                  e.getFromStatus() + " -> " + e.getToStatus() + ": " + e.getReason(),
                  null,
                  null,
                  e.getCreatedBy(),
                  e.getCreatedAt(),
                  e.getCompanyId(),
                  instruments
                      .findById(e.getInstrumentId())
                      .map(i -> DisbursementSettings.voucherLink(i.getVoucherId()))
                      .orElse("/disbursement")));
        }
      }
    }
    return items;
  }

  private void vouchers(ApprovalViewer viewer, List<PendingApproval> items) {
    for (Voucher v : vouchers.findByStageInOrderByIdAsc(List.of(VoucherStage.FOR_APPROVAL))) {
      if (viewer.mayApproveItemOf(v.getSubmittedBy())
          && viewer.mayApproveItemOf(v.getReviewedBy())) {
        items.add(
            new PendingApproval(
                MODULE,
                "Disbursement voucher",
                v.getDvNo(),
                v.getPayeeName() + " - " + v.getDisbursementType(),
                v.getNet(),
                v.getCurrency(),
                v.getSubmittedBy() == null ? v.getCreatedBy() : v.getSubmittedBy(),
                v.getUpdatedAt() == null ? v.getCreatedAt() : v.getUpdatedAt(),
                v.getCompanyId(),
                DisbursementSettings.voucherLink(v.getId())));
      }
    }
  }

  private void payees(ApprovalViewer viewer, List<PendingApproval> items) {
    for (Payee p :
        payees.findByStageInOrderByIdAsc(
            EnumSet.of(
                PayeeStage.FOR_AUTHORIZATION,
                PayeeStage.FOR_DEACTIVATION,
                PayeeStage.FOR_REACTIVATION))) {
      String maker = p.getUpdatedBy() == null ? p.getCreatedBy() : p.getUpdatedBy();
      if (viewer.mayApproveItemOf(maker)) {
        items.add(
            new PendingApproval(
                MODULE,
                "Payee",
                p.getPayeeCode(),
                p.getName() + " (" + p.getStage() + ")",
                null,
                null,
                maker,
                p.getUpdatedAt() == null ? p.getCreatedAt() : p.getUpdatedAt(),
                p.getCompanyId(),
                DisbursementSettings.payeeLink(p.getId())));
      }
    }
  }

  private void fundings(ApprovalViewer viewer, List<PendingApproval> items) {
    for (FundingRequest f :
        fundings.findByStageInOrderByIdAsc(
            EnumSet.of(
                FundingStage.FOR_VERIFICATION,
                FundingStage.FOR_APPROVAL_1,
                FundingStage.FOR_APPROVAL_2))) {
      String permission =
          f.getStage() == FundingStage.FOR_VERIFICATION
              ? "DISB_FUNDING_VERIFY"
              : "DISB_FUNDING_APPROVE";
      boolean mine =
          !viewer.mayApproveItemOf(f.getCreatedBy())
              || !viewer.mayApproveItemOf(f.getVerifiedBy())
              || !viewer.mayApproveItemOf(f.getFirstApprover());
      if (viewer.can(permission) && !mine) {
        items.add(
            new PendingApproval(
                MODULE,
                "Account funding",
                f.getFundingNo(),
                f.getPurpose() + " (" + f.getStage() + ")",
                f.getAmount(),
                f.getCurrency(),
                f.getCreatedBy(),
                f.getCreatedAt(),
                f.getCompanyId(),
                DisbursementSettings.fundingLink(f.getId())));
      }
    }
  }
}
