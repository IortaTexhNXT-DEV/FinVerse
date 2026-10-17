package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import org.springframework.stereotype.Component;

/**
 * The letters of the eligibility matrix (Annex M) served by a template of their own: the SFU letter
 * as the closing letter of a mortgaged Motor account, and the FFY Reminder as the first notice of
 * an account tagged Free First Year.
 */
@Component
public class AnnexTemplates {

  private final AccountRepository accounts;

  /**
   * Creates the choice.
   *
   * @param accounts the expiring accounts (free first year)
   */
  public AnnexTemplates(AccountRepository accounts) {
    this.accounts = accounts;
  }

  /** The SFU letter and the FFY Reminder of Annex M, or null. */
  String of(RenewalCandidate c, LetterType type, RaNotice notice) {
    if (type == LetterType.NAL && isMortgaged(c) && motor(c)) {
      return RenewalCodes.TEMPLATE_SFU;
    }
    boolean firstNotice = type == LetterType.RA && notice != RaNotice.SECOND;
    return firstNotice && freeFirstYearAccount(c) ? RenewalCodes.TEMPLATE_RA_FFY : null;
  }

  /** Annex M: the closing letter of a mortgaged Motor account is the SFU letter. */
  private static boolean motor(RenewalCandidate c) {
    var p = c.getSnapshot().product();
    return p != null && "MOTOR".equals(p.lineCode());
  }

  /** Annex M: the first notice of an account tagged Free First Year is the FFY Reminder. */
  private boolean freeFirstYearAccount(RenewalCandidate c) {
    return c.getExpiringArn() != null
        && accounts
            .findByArn(c.getExpiringArn())
            .map(a -> a.getFreeFirstYear() != null && a.getFreeFirstYear().active())
            .orElse(false);
  }

  private static boolean isMortgaged(RenewalCandidate c) {
    return c.getSnapshot().mortgage() != null && c.getSnapshot().mortgage().mortgaged();
  }
}
