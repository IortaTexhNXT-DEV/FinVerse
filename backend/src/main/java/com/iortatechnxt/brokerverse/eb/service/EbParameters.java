package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Employee Benefits parameters (sys_parameter category EMPLOYEE_BENEFITS, V1030; design 8.3): every
 * lead time, reminder, TAT target and switch the EB services use. All defaults are placeholders
 * until EBQ02, EBQ07, EBQ10, EBQ16 and EBQ20 are answered. Shared by the build waves; changed only
 * by additions.
 */
@Component
public class EbParameters {

  /** Days before expiry the RA is sent and the renewal cycle opened (BRID-001). */
  public static final String RA_LEAD_DAYS = "EB_RA_LEAD_DAYS";

  /** Days before expiry of the RA reminders (BRID-002). */
  public static final String RA_REMINDER_DAYS = "EB_RA_REMINDER_DAYS";

  /** Working days an insurer has to answer a request (TAT). */
  public static final String PROPOSAL_REPLY_DAYS = "EB_PROPOSAL_REPLY_DAYS";

  /** Working days for the franchise decision (BRID-027). */
  public static final String FRANCHISE_TAT_DAYS = "EB_FRANCHISE_TAT_DAYS";

  /** Working days to advise the client of the franchise outcome (BRID-029). */
  public static final String FRANCHISE_ADVICE_DAYS = "EB_FRANCHISE_ADVICE_DAYS";

  /** Working days from the last proposal to the comparative (TAT). */
  public static final String COMPARATIVE_DAYS = "EB_COMPARATIVE_DAYS";

  /** Working days between follow-ups of a tracked item (BRID-030). */
  public static final String FOLLOWUP_DAYS = "EB_FOLLOWUP_DAYS";

  /** Follow-ups before escalation (BRID-030). */
  public static final String FOLLOWUP_MAX = "EB_FOLLOWUP_MAX";

  /** Endorsement request only after the member change billing is paid (EBQ16). */
  public static final String ADJ_BOOKING_REQUIRES_PAYMENT = "EB_ADJ_BOOKING_REQUIRES_PAYMENT";

  private static final int DEFAULT_RA_LEAD = 135;
  private static final int DEFAULT_REPLY = 5;
  private static final int DEFAULT_ADVICE = 2;
  private static final int DEFAULT_COMPARATIVE = 3;
  private static final int DEFAULT_FOLLOWUP_MAX = 3;

  private final SystemParameterService parameters;

  /**
   * Creates the parameters.
   *
   * @param parameters business parameters
   */
  public EbParameters(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Days before expiry the renewal advice is sent.
   *
   * @return days
   */
  public int raLeadDays() {
    return parameters.intValue(RA_LEAD_DAYS, DEFAULT_RA_LEAD);
  }

  /**
   * Days before expiry of the RA reminders, largest first (earliest reminder first).
   *
   * @return days
   */
  public List<Integer> raReminderDays() {
    return parameters.items(RA_REMINDER_DAYS).stream()
        .map(Integer::valueOf)
        .sorted(Comparator.reverseOrder())
        .toList();
  }

  /**
   * Working days an insurer has to answer a request.
   *
   * @return days
   */
  public int proposalReplyDays() {
    return parameters.intValue(PROPOSAL_REPLY_DAYS, DEFAULT_REPLY);
  }

  /**
   * Working days for the insurer's franchise decision.
   *
   * @return days
   */
  public int franchiseTatDays() {
    return parameters.intValue(FRANCHISE_TAT_DAYS, DEFAULT_REPLY);
  }

  /**
   * Working days to advise the client of the franchise outcome.
   *
   * @return days
   */
  public int franchiseAdviceDays() {
    return parameters.intValue(FRANCHISE_ADVICE_DAYS, DEFAULT_ADVICE);
  }

  /**
   * Working days from the last proposal to the comparative.
   *
   * @return days
   */
  public int comparativeDays() {
    return parameters.intValue(COMPARATIVE_DAYS, DEFAULT_COMPARATIVE);
  }

  /**
   * Working days between follow-ups of a tracked item.
   *
   * @return days
   */
  public int followUpDays() {
    return parameters.intValue(FOLLOWUP_DAYS, DEFAULT_REPLY);
  }

  /**
   * Follow-ups before a tracked item is escalated.
   *
   * @return count
   */
  public int followUpMax() {
    return parameters.intValue(FOLLOWUP_MAX, DEFAULT_FOLLOWUP_MAX);
  }

  /**
   * Whether the endorsement request of a member change waits for its billing to be paid.
   *
   * @return true when payment comes first
   */
  public boolean adjustmentRequiresPayment() {
    return Boolean.parseBoolean(parameters.text(ADJ_BOOKING_REQUIRES_PAYMENT, "false").strip());
  }

  /**
   * The target of a TAT activity (working days; days before inception for the renewal advice).
   *
   * @param activity activity
   * @return days
   */
  public int tatDays(TatActivity activity) {
    return parameters.intValue(activity.parameter(), DEFAULT_REPLY);
  }
}
