package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalExtractionRunRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * The rules applied to the draft of a renewal account before it is created: the term of a renewal
 * account created by hand (FRRN.005.01: inception one day after the expiry of the expiring account,
 * {@value #OFFSET}, and expiry one year later) and the CBG Motor automatic values (FRRN.009.01).
 */
@Component
public class RenewalDraftRules {

  /** Parameter: days between the expiry and the inception of a manually created renewal. */
  public static final String OFFSET = "RNW_MANUAL_INCEPTION_OFFSET_DAYS";

  private final RenewalExtractionRunRepository runs;
  private final SystemParameterService parameters;
  private final CbgMotorAutoUpdate autoUpdate;

  /**
   * Creates the rules.
   *
   * @param runs extraction runs (manual creation)
   * @param parameters system parameters
   * @param autoUpdate CBG Motor automatic values
   */
  public RenewalDraftRules(
      RenewalExtractionRunRepository runs,
      SystemParameterService parameters,
      CbgMotorAutoUpdate autoUpdate) {
    this.runs = runs;
    this.parameters = parameters;
    this.autoUpdate = autoUpdate;
  }

  /**
   * The draft with the rules applied.
   *
   * @param c renewal
   * @param draft draft from the expiring account
   * @return draft to create
   */
  public AccountDraft apply(RenewalCandidate c, AccountDraft draft) {
    AccountDraft d = autoUpdate.apply(c, draft);
    if (!manual(c) || d.periodFrom() == null) {
      return d;
    }
    LocalDate from = c.getExpiryDate().plusDays(parameters.intValue(OFFSET, 1));
    return new AccountDraft(
        d.clientId(),
        d.productCode(),
        d.marketSegment(),
        d.sourceChannel(),
        d.insurerCode(),
        d.insurerBranch(),
        from,
        from.plusYears(1),
        d.multiYear(),
        d.termYears(),
        d.currency(),
        d.paymentArrangement(),
        d.mortgage(),
        d.contact(),
        d.items(),
        d.ratingBasis(),
        d.commissionRate(),
        d.ffyStart());
  }

  private boolean manual(RenewalCandidate c) {
    return c.getExtractionRunId() != null
        && runs.findById(c.getExtractionRunId())
            .map(r -> r.getTrigger() == ExtractionTrigger.MANUAL_ACCOUNT)
            .orElse(false);
  }
}
