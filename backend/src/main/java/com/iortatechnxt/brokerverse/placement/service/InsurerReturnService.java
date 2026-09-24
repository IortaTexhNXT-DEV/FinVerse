package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountStatusChanged;
import com.iortatechnxt.brokerverse.placement.domain.InsurerReturn;
import com.iortatechnxt.brokerverse.placement.domain.InsurerReturnRepository;
import com.iortatechnxt.brokerverse.placement.domain.SlipAccount;
import java.time.Clock;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer returns and the return loop (BRNB.033/034/058/059): Processing captures the insurer's
 * return with a reason (list RETURN_REASON) and remarks ({@code insurer_return}); the account is
 * then resubmitted for placement after the correction, or returned to Marketing with the generic
 * {@code return} action of the workflow panel, which Marketing resubmits to Processing. The
 * resolution of each return is recorded from the account's status change.
 */
@Service
@Transactional
public class InsurerReturnService {

  private final InsurerReturnRepository returns;
  private final PlacementAccounts accounts;
  private final AccountLifecycleService lifecycle;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param returns insurer returns
   * @param accounts account look-ups
   * @param lifecycle account lifecycle
   * @param clock clock
   */
  public InsurerReturnService(
      InsurerReturnRepository returns,
      PlacementAccounts accounts,
      AccountLifecycleService lifecycle,
      Clock clock) {
    this.returns = returns;
    this.accounts = accounts;
    this.lifecycle = lifecycle;
    this.clock = clock;
  }

  /**
   * Captures a placement returned by the insurer.
   *
   * @param arn placed account
   * @param reasonCode reason (list RETURN_REASON)
   * @param remarks insurer remarks
   * @return the return
   */
  public InsurerReturn recordReturn(String arn, String reasonCode, String remarks) {
    Account account = accounts.require(arn);
    lifecycle.recordInsurerReturn(arn, reasonCode, remarks);
    return returns.save(
        new InsurerReturn(
            account.getCompanyId(),
            new SlipAccount(account.getId(), arn),
            account.getInsurerCode(),
            account.getLifecycle().getPlacementSlipRef(),
            reasonCode,
            remarks));
  }

  /**
   * Resubmits a returned placement after the correction (back to Ready for placement; regenerate
   * the slip and send it again).
   *
   * @param arn account returned by the insurer
   * @param comment what was corrected
   * @return the account
   */
  public Account resubmit(String arn, String comment) {
    return lifecycle.resubmitPlacement(arn, comment);
  }

  /**
   * Returns of an account, newest first.
   *
   * @param arn Account Reference Number
   * @return returns
   */
  @Transactional(readOnly = true)
  public List<InsurerReturn> returnsOf(String arn) {
    return returns.findByArnOrderByIdDesc(arn);
  }

  /**
   * Records how an open return was resolved when the account leaves Returned by insurer.
   *
   * @param event account status change
   */
  @EventListener
  public void on(AccountStatusChanged event) {
    if (event.from() != AccountStatus.RETURNED_BY_INSURER) {
      return;
    }
    returns
        .findFirstByArnAndResolvedAtIsNullOrderByIdDesc(event.arn())
        .ifPresent(r -> r.resolve(event.action(), clock.instant()));
  }
}
