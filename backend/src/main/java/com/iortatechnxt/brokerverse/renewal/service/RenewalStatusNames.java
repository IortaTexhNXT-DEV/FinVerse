package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The renewal status as users read it on the dashboards and lists: BDOI's status names (In Process
 * Renewal, Submitted for Posting, Posted, For RA Generation, Submitted for Placement, For Booking,
 * Booked, Returned - Renewal ...) or the names of the workflow stages, chosen by the parameter
 * {@value #PARAMETER} (CLIENT, by default, for these names).
 */
@Component
public class RenewalStatusNames {

  /** Parameter: CLIENT or SYSTEM. */
  public static final String PARAMETER = "RNW_STATUS_NAMES";

  /** Value of the parameter for the workflow stage names. */
  public static final String SYSTEM = "SYSTEM";

  private static final String IN_PROCESS = "In Process Renewal";
  private static final String POSTED = "Posted";
  private static final String RA_SENT = "RA Sent";

  private static final Map<RenewalStage, String> BDOI =
      Map.ofEntries(
          Map.entry(RenewalStage.EXTRACTED, IN_PROCESS),
          Map.entry(RenewalStage.EVALUATING, IN_PROCESS),
          Map.entry(RenewalStage.UNASSIGNED, IN_PROCESS),
          Map.entry(RenewalStage.FOR_DISPOSITION, IN_PROCESS),
          Map.entry(RenewalStage.TRANSFER_PENDING, "Transfer Pending"),
          Map.entry(RenewalStage.FOR_TL_REVIEW, "Submitted for Posting"),
          Map.entry(RenewalStage.NB_PATH, "For Quotation / Proposal"),
          Map.entry(RenewalStage.FOR_PROCESSING, POSTED),
          Map.entry(RenewalStage.IN_PROCESSING, POSTED),
          Map.entry(RenewalStage.WITH_INSURER, "For Insurer Disposition"),
          Map.entry(RenewalStage.RA_READY, "For RA Generation"),
          Map.entry(RenewalStage.RA_GENERATED, "RA Generated"),
          Map.entry(RenewalStage.RA_SENT, RA_SENT),
          Map.entry(RenewalStage.ACCEPTED, "Submitted for Approval"),
          Map.entry(RenewalStage.FOR_PLACEMENT_BOOKING, "Submitted for Placement"),
          Map.entry(RenewalStage.LETTER_PENDING, "Not for Renewal"),
          Map.entry(RenewalStage.RENEWED, "Booked"),
          Map.entry(RenewalStage.CLOSED, "Closed"));

  private static final Map<String, String> PLACEMENT_NAMES =
      Map.of(
          "PLACED", "For Booking",
          "POLICY_ISSUED", "For Booking",
          "FOR_BOOKING", "For Booking",
          "RETURNED_BY_INSURER", "Rejected Placement",
          "REJECTED_PLACEMENT", "Rejected Placement",
          "BOOKED", "Booked",
          "FOR_EPOLICY_SENDING", "For E-Policy Sending",
          "EPOLICY_SENT", "E-Policy Sent");

  private final SystemParameterService parameters;

  /**
   * Creates the names.
   *
   * @param parameters system parameters
   */
  public RenewalStatusNames(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * The status of a renewal.
   *
   * @param stage stage code
   * @param returned whether the renewal was returned and not yet submitted again
   * @param accountStatus status of its renewal account, or null
   * @return the name
   */
  public String of(String stage, boolean returned, String accountStatus) {
    RenewalStage s = RenewalStage.valueOf(stage);
    if (SYSTEM.equals(parameters.text(PARAMETER, "CLIENT").strip())) {
      return s.label();
    }
    return bdoi(s, returned, accountStatus);
  }

  /**
   * BDOI's name of a status.
   *
   * @param s stage
   * @param returned returned flag
   * @param accountStatus status of the renewal account or null
   * @return name
   */
  static String bdoi(RenewalStage s, boolean returned, String accountStatus) {
    if (returned && s.isOpen() && !s.isMarketingLocked()) {
      return "Returned - Renewal";
    }
    if (s == RenewalStage.FOR_PLACEMENT_BOOKING && accountStatus != null) {
      return PLACEMENT_NAMES.getOrDefault(accountStatus, BDOI.get(s));
    }
    return BDOI.get(s);
  }
}
