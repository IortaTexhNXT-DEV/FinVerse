package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.booking.domain.QueueSource;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Auto-book (BRNB.076): when an account's NB_ACCOUNT case enters POLICY_ISSUED (e-policy received,
 * or direct booking), an account matching an enabled auto-book rule is queued for the next booking
 * batch - never booked inside the issuing transaction. Accounts without a rule stay in the manual
 * "Ready to book" list.
 */
@Component
public class AutoBookListener {

  private static final String WORKFLOW = "NB_ACCOUNT";
  private static final String POLICY_ISSUED = "POLICY_ISSUED";

  private final AccountQueryService accounts;
  private final BookingRuleService rules;
  private final BookingQueueService queue;

  /**
   * Creates the listener.
   *
   * @param accounts account reads
   * @param rules auto-book rules
   * @param queue booking queue
   */
  public AutoBookListener(
      AccountQueryService accounts, BookingRuleService rules, BookingQueueService queue) {
    this.accounts = accounts;
    this.rules = rules;
    this.queue = queue;
  }

  /**
   * Queues an issued account when an auto-book rule matches it.
   *
   * @param event stage change
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!WORKFLOW.equals(event.workflowCode()) || !POLICY_ISSUED.equals(event.toStage())) {
      return;
    }
    Account account = accounts.get(Long.valueOf(event.entityId()));
    if (rules.autoBooks(
        account.getCompanyId(), account.getProductCode(), account.getMarketSegment())) {
      queue.enqueueIssued(account, QueueSource.AUTO);
    }
  }
}
