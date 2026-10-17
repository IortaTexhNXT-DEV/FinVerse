package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.StatusEditStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.StatusEditRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the stage of the Disbursement workflows on their records, for every transition including
 * the generic ones run from the workflow panel (return of a voucher, rejection of a status edit,
 * return or decline of a funding request, return of a payee): vouchers ({@code DISB_VOUCHER}),
 * payees ({@code DISB_PAYEE}), funding requests ({@code DISB_FUNDING}) and status edits ({@code
 * DISB_STATUS_EDIT}).
 */
@Component
public class DisbursementStageListener {

  private final VoucherRepository vouchers;
  private final PayeeRepository payees;
  private final FundingRequestRepository fundings;
  private final StatusEditRepository edits;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the listener.
   *
   * @param vouchers vouchers
   * @param payees payees
   * @param fundings funding requests
   * @param edits status edits
   * @param currentUser current user
   * @param clock clock
   */
  public DisbursementStageListener(
      VoucherRepository vouchers,
      PayeeRepository payees,
      FundingRequestRepository fundings,
      StatusEditRepository edits,
      CurrentUser currentUser,
      Clock clock) {
    this.vouchers = vouchers;
    this.payees = payees;
    this.fundings = fundings;
    this.edits = edits;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Mirrors a stage change.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    switch (event.entityType()) {
      case DisbursementSettings.VOUCHER ->
          vouchers
              .findById(id(event))
              .ifPresent(v -> v.markStage(VoucherStage.valueOf(event.toStage())));
      case DisbursementSettings.PAYEE ->
          payees
              .findById(id(event))
              .ifPresent(p -> p.markStage(PayeeStage.valueOf(event.toStage())));
      case DisbursementSettings.FUNDING ->
          fundings
              .findById(id(event))
              .ifPresent(f -> f.markStage(FundingStage.valueOf(event.toStage())));
      case DisbursementSettings.STATUS_EDIT ->
          edits
              .findById(id(event))
              .ifPresent(
                  e ->
                      e.decided(
                          StatusEditStage.valueOf(event.toStage()),
                          currentUser.username(),
                          clock.instant()));
      default -> {
        // another module's record
      }
    }
  }

  private static Long id(WorkCaseTransitioned event) {
    return Long.valueOf(event.entityId());
  }
}
