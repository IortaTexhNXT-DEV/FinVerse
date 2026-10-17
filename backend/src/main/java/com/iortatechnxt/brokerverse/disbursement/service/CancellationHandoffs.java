package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Closes the check-cancellation hand-offs of Payment Requests (MKT 1.19.0, DIS 2.20.0) when the
 * Disbursement approver cancels the DV they name: Payment Requests records an approved check
 * cancellation on port {@code DV_CANCELLATION} for team {@code DISB_APPROVE} with the DV number as
 * reference, and the approver cancels that DV on the voucher record.
 */
@Component
public class CancellationHandoffs {

  /** Port of the check-cancellation hand-off (payrequest {@code DisbursementLink.CANCEL_PORT}). */
  public static final String PORT = "DV_CANCELLATION";

  private static final int PAGE = 200;

  private final VoucherRepository vouchers;
  private final HandoffService handoffs;

  /**
   * Creates the component.
   *
   * @param vouchers vouchers
   * @param handoffs Operations hand-offs
   */
  public CancellationHandoffs(VoucherRepository vouchers, HandoffService handoffs) {
    this.vouchers = vouchers;
    this.handoffs = handoffs;
  }

  /**
   * Closes the open hand-offs of a voucher that was just cancelled.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!DisbursementSettings.VOUCHER.equals(event.entityType())
        || !VoucherStage.CANCELLED.name().equals(event.toStage())) {
      return;
    }
    vouchers.findById(Long.valueOf(event.entityId())).ifPresent(this::close);
  }

  private void close(Voucher v) {
    handoffs
        .list(v.getCompanyId(), OpsHandoff.Status.OPEN, PageRequest.of(0, PAGE))
        .getContent()
        .stream()
        .filter(h -> PORT.equals(h.getPort()) && v.getDvNo().equals(h.getReference()))
        .forEach(
            h -> handoffs.close(h.getId(), "DV " + v.getDvNo() + " cancelled in Disbursement"));
  }
}
