package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import java.util.Map;

/**
 * The status names of BDOI's FRS for the quotation requests of non-package products (FRPM.005.01)
 * and the package requests (FRPM.011.01), from the workflow stage and the last workflow action (a
 * request sent back shows "Returned for Revision", a quotation slip sent back "QS Returned", a
 * package version sent back to MBS "Package Deployment Returned").
 */
public final class PmStatusNames {

  /** Name of a request sent back to its creator. */
  public static final String RETURNED = "Returned for Revision";

  private static final String RETURN = "return";
  private static final String UNDER_NEGOTIATION = "Under Negotiation";
  private static final String TERMS_AGREED = "Terms Agreed";
  private static final String PROPOSAL_READY = "Proposal Ready";
  private static final String FOR_TSU_REVIEW = "For TSU Review";
  private static final String REJECTED = "Rejected";

  private static final Map<String, String> QUOTATION =
      Map.ofEntries(
          Map.entry("DRAFT", "Draft"),
          Map.entry("FOR_MKT_APPROVAL", "Pending Marketing Approval"),
          Map.entry("WITH_TSU", FOR_TSU_REVIEW),
          Map.entry("QS_PREPARATION", "For Quotation Slip Creation"),
          Map.entry("QS_FOR_APPROVAL", "QS for Approval"),
          Map.entry("QS_SENT", UNDER_NEGOTIATION),
          Map.entry("TERMS_RECEIVED", UNDER_NEGOTIATION),
          Map.entry("PS_FOR_APPROVAL", TERMS_AGREED),
          Map.entry("PS_RELEASED", PROPOSAL_READY),
          Map.entry("SENT_TO_CLIENT", "Proposal Released"),
          Map.entry("ACCEPTED", "For Deployment"),
          Map.entry("CONVERTED", "Deployed"),
          Map.entry("NOT_PROCEEDED", "Proposal Rejected"),
          Map.entry("VOIDED", REJECTED));

  private static final Map<RequestStage, String> PACKAGE =
      Map.ofEntries(
          Map.entry(RequestStage.DRAFT, "Draft"),
          Map.entry(RequestStage.FOR_MKT_APPROVAL, "Pending Marketing TL Approval"),
          Map.entry(RequestStage.FOR_TSU_REVIEW, FOR_TSU_REVIEW),
          Map.entry(RequestStage.FOR_TSU_APPROVAL, FOR_TSU_REVIEW),
          Map.entry(RequestStage.NEGOTIATION, UNDER_NEGOTIATION),
          Map.entry(RequestStage.TERMS_REVIEW, TERMS_AGREED),
          Map.entry(RequestStage.FOR_MKT_REVIEW, PROPOSAL_READY),
          Map.entry(RequestStage.REQUIREMENTS_PREP, PROPOSAL_READY),
          Map.entry(RequestStage.FOR_MANCOM, "Awaiting Mancom Approval"),
          Map.entry(RequestStage.MANCOM_APPROVED, "Approved, Deployment to Request"),
          Map.entry(RequestStage.WITH_MBS, "For Deployment"),
          Map.entry(RequestStage.FOR_VALIDATION, "Package Deployment for Review"),
          Map.entry(RequestStage.RELEASED, "Deployed"),
          Map.entry(RequestStage.RETIRED, "Deactivated"),
          Map.entry(RequestStage.NOT_PROCEEDED, REJECTED),
          Map.entry(RequestStage.VOIDED, REJECTED));

  private static final Map<String, String> SENT_BACK =
      Map.of(
          "DRAFT", RETURNED,
          "QS_PREPARATION", "QS Returned",
          "TERMS_RECEIVED", "Proposal Returned");

  private PmStatusNames() {}

  /**
   * The name of a quotation request status.
   *
   * @param status proposal status code
   * @param lastAction last workflow action, may be null
   * @param termsClosed whether the insurer terms were closed (insurers selected)
   * @return status name
   */
  public static String quotation(String status, String lastAction, boolean termsClosed) {
    if (status == null) {
      return "";
    }
    if (RETURN.equals(lastAction) && SENT_BACK.containsKey(status)) {
      return SENT_BACK.get(status);
    }
    if ("TERMS_RECEIVED".equals(status)) {
      return termsReceived(lastAction, termsClosed);
    }
    return QUOTATION.getOrDefault(status, status);
  }

  private static String termsReceived(String lastAction, boolean termsClosed) {
    if ("client_return".equals(lastAction)) {
      return RETURNED;
    }
    return termsClosed ? TERMS_AGREED : UNDER_NEGOTIATION;
  }

  /**
   * The name of a package request stage.
   *
   * @param stage stage
   * @param lastAction last workflow action, may be null
   * @return status name
   */
  public static String packageRequest(RequestStage stage, String lastAction) {
    return packageRequest(stage, lastAction, 0);
  }

  /**
   * The name of a package request stage with the Marketing approvals already given (Pending
   * Marketing TL, TH or UH Approval).
   *
   * @param stage stage
   * @param lastAction last workflow action, may be null
   * @param marketingLevel Marketing approvals given
   * @return status name
   */
  public static String packageRequest(RequestStage stage, String lastAction, int marketingLevel) {
    if (stage == RequestStage.FOR_MKT_APPROVAL && marketingLevel > 0) {
      return marketingLevel == 1
          ? "Pending Marketing TH Approval"
          : "Pending Marketing UH Approval";
    }
    return named(stage, lastAction);
  }

  private static String named(RequestStage stage, String lastAction) {
    if (stage == null) {
      return "";
    }
    if (stage == RequestStage.DRAFT && RETURN.equals(lastAction)) {
      return RETURNED;
    }
    if (stage == RequestStage.WITH_MBS && "version_returned".equals(lastAction)) {
      return "Package Deployment Returned";
    }
    return PACKAGE.getOrDefault(stage, stage.name());
  }
}
