package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The business type, pipeline stage and pipeline tier of a dashboard account (FRRN.002.02.02 and
 * .04). Business types: New (a client without an earlier booked account), Organic (an existing
 * client), Submitted (an account from a submitted policy), Renewal, and Deferred (an open renewal
 * past its expiry date, carried under a hold cover or still in progress). The stages are read from
 * the parameters {@value #RENEWAL_STAGES} and {@value #NB_STAGES}; a booked account stays in For
 * Delivery until its policy is sent to the client. The tiers of the closing ratio: Tier 3 in
 * process (before posting), Tier 2 posted, Tier 1 booked.
 */
@Component
public class PipelineRules {

  /** Parameter: renewal statuses per pipeline stage. */
  public static final String RENEWAL_STAGES = "RNW_PIPELINE_STAGES";

  /** Parameter: New Business statuses per pipeline stage. */
  public static final String NB_STAGES = "RNW_PIPELINE_NB_STAGES";

  /** Business types in display order. */
  public static final List<String> CATEGORIES =
      List.of("NEW", "ORGANIC", "SUBMITTED", "RENEWAL", "DEFERRED");

  /** Pipeline stages in display order. */
  public static final List<String> STAGES =
      List.of("FOR_SURVEY", "FOR_PROPOSAL", "FOR_CONFIRM", "FOR_BOOKING", "FOR_DELIVERY");

  /** Tier 3. */
  public static final String IN_PROCESS = "IN_PROCESS";

  /** Tier 2. */
  public static final String POSTED = "POSTED";

  /** Tier 1. */
  public static final String BOOKED = "BOOKED";

  private static final String DELIVERY = "FOR_DELIVERY";

  private static final Set<String> RENEWAL_IN_PROCESS =
      Set.of(
          "EXTRACTED",
          "EVALUATING",
          "UNASSIGNED",
          "TRANSFER_PENDING",
          "FOR_DISPOSITION",
          "FOR_TL_REVIEW",
          "NB_PATH");

  private static final Set<String> RENEWAL_POSTED =
      Set.of(
          "FOR_PROCESSING",
          "IN_PROCESSING",
          "WITH_INSURER",
          "RA_READY",
          "RA_GENERATED",
          "RA_SENT",
          "ACCEPTED",
          "FOR_PLACEMENT_BOOKING");

  private static final Set<String> NB_IN_PROCESS =
      Set.of("DRAFT", "SUBMITTED", "RETURNED_TO_MARKETING");

  private static final Set<String> NB_POSTED =
      Set.of(
          "AWAITING_PAYMENT",
          "READY_FOR_PLACEMENT",
          "PLACED",
          "RETURNED_BY_INSURER",
          "POLICY_ISSUED");

  private static final String DEFAULT_RENEWAL =
      "FOR_SURVEY:EXTRACTED|EVALUATING|UNASSIGNED|TRANSFER_PENDING|FOR_DISPOSITION;"
          + "FOR_PROPOSAL:FOR_TL_REVIEW|NB_PATH|FOR_PROCESSING|IN_PROCESSING|WITH_INSURER;"
          + "FOR_CONFIRM:RA_READY|RA_GENERATED|RA_SENT|ACCEPTED;FOR_BOOKING:FOR_PLACEMENT_BOOKING;"
          + "FOR_DELIVERY:RENEWED";

  private static final String DEFAULT_NB =
      "FOR_SURVEY:DRAFT|RETURNED_TO_MARKETING;FOR_PROPOSAL:SUBMITTED;FOR_CONFIRM:AWAITING_PAYMENT;"
          + "FOR_BOOKING:READY_FOR_PLACEMENT|PLACED|RETURNED_BY_INSURER|POLICY_ISSUED;"
          + "FOR_DELIVERY:BOOKED";

  private final SystemParameterService parameters;

  /**
   * Creates the rules.
   *
   * @param parameters system parameters
   */
  public PipelineRules(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Sets the business type ({@code category}), the pipeline stage ({@code pstage}, null when the
   * account is out of the pipeline) and the tier ({@code tier}) of each item.
   *
   * @param items dashboard items
   * @param today business date
   */
  public void classify(List<DashboardItem> items, LocalDate today) {
    Map<String, String> renewal = map(parameters.text(RENEWAL_STAGES, DEFAULT_RENEWAL));
    Map<String, String> nb = map(parameters.text(NB_STAGES, DEFAULT_NB));
    for (DashboardItem i : items) {
      String stage = i.stage();
      String pstage = (i.renewal() ? renewal : nb).get(stage);
      if (DELIVERY.equals(pstage) && i.flag("delivered")) {
        pstage = null;
      }
      i.put("pstage", pstage);
      i.put("category", category(i, today));
      i.put("tier", tier(i));
    }
  }

  private static String category(DashboardItem i, LocalDate today) {
    if (i.renewal()) {
      LocalDate expiry = i.date("expiry_date");
      return i.open() && expiry != null && expiry.isBefore(today) ? "DEFERRED" : "RENEWAL";
    }
    if ("SUBMITTED_POLICY".equals(i.text("origin"))) {
      return "SUBMITTED";
    }
    return i.flag("existing_client") ? "ORGANIC" : "NEW";
  }

  private static String tier(DashboardItem i) {
    String s = i.stage();
    if (i.renewal()) {
      if ("RENEWED".equals(s)) {
        return BOOKED;
      }
      return RENEWAL_IN_PROCESS.contains(s)
          ? IN_PROCESS
          : RENEWAL_POSTED.contains(s) ? POSTED : null;
    }
    if ("BOOKED".equals(s)) {
      return BOOKED;
    }
    return NB_IN_PROCESS.contains(s) ? IN_PROCESS : NB_POSTED.contains(s) ? POSTED : null;
  }

  /**
   * Reads a mapping "STAGE:STATUS|STATUS;STAGE:..." into status to stage.
   *
   * @param text parameter value
   * @return stage by status
   */
  static Map<String, String> map(String text) {
    Map<String, String> result = new HashMap<>();
    for (String part : text.split(";")) {
      String[] kv = part.split(":", 2);
      if (kv.length == 2) {
        for (String status : kv[1].split("\\|")) {
          if (!status.isBlank()) {
            result.put(status.strip(), kv[0].strip());
          }
        }
      }
    }
    return result;
  }
}
