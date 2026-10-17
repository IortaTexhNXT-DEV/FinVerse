package com.iortatechnxt.brokerverse.eb.domain;

/**
 * Activities of the TAT annex (BRD p.42; spec 6.3) stamped in {@code eb_activity_log} and measured
 * by the report {@code EB-TAT} against their parameter (working days; days before inception for the
 * renewal advice).
 */
public enum TatActivity {
  /** Submission of the franchise request upon complete documents. */
  FRANCHISE_SUBMIT("EB_TAT_FRANCHISE_SUBMIT"),
  /** Franchise decision by the insurer. */
  FRANCHISE_DECISION("EB_FRANCHISE_TAT_DAYS"),
  /** Request for quotation upon franchise approval. */
  RFQ_SUBMIT("EB_TAT_RFQ_SUBMIT"),
  /** Renewal advice and renewal requirements (days before inception). */
  RENEWAL_ADVICE("EB_RA_LEAD_DAYS"),
  /** Quotation / proposal issued by the insurer. */
  QUOTATION("EB_PROPOSAL_REPLY_DAYS"),
  /** Proposal and comparative sent to the client. */
  PROPOSAL_TO_CLIENT("EB_COMPARATIVE_DAYS"),
  /** Client confirmation sent to the insurer. */
  CONFIRMATION("EB_TAT_CONFIRMATION"),
  /** Certificate of cover issued by the insurer. */
  CERTIFICATE_OF_COVER("EB_TAT_COC"),
  /** Request for placement and booking. */
  PLACEMENT_REQUEST("EB_TAT_PLACEMENT_REQUEST"),
  /** Placement slip submitted by Processing. */
  PLACEMENT_SLIP("EB_TAT_PLACEMENT_SLIP"),
  /** Policy / contract and billing / SOA from the insurer. */
  POLICY_SOA("EB_TAT_POLICY_SOA"),
  /** SOA validated by Processing. */
  SOA_VALIDATION("EB_TAT_SOA_VALIDATION"),
  /** Booking once billing is validated. */
  BOOKING("EB_TAT_BOOKING"),
  /** Policy / contract checked against the proposal. */
  POLICY_CHECK("EB_TAT_POLICY_CHECK"),
  /** SOA released to Collection, policy to the client. */
  RELEASE("EB_TAT_RELEASE"),
  /** Inclusion / deletion step. */
  MEMBER_CHANGE("EB_TAT_MEMBER_CHANGE"),
  /** Premium collection upon SOA / billing receipt. */
  COLLECTION("EB_TAT_COLLECTION"),
  /** Member cards submitted, validated and released. */
  CARDS("EB_TAT_CARDS"),
  /** Official receipt from the insurer. */
  OFFICIAL_RECEIPT("EB_TAT_OR");

  private final String parameter;

  TatActivity(String parameter) {
    this.parameter = parameter;
  }

  /**
   * The parameter holding the target of the activity.
   *
   * @return {@code sys_parameter} key
   */
  public String parameter() {
    return parameter;
  }
}
