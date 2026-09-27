package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;

/**
 * Document types and process tags of Employee Benefits (spec section 6.1, BRID-007, 008, 021, 025,
 * 026, 030; values of list {@code DOCUMENT_TYPE} seeded by V1030 / V1031, access classes in {@code
 * att_document_access}). Shared by the build waves; changed only by additions.
 */
public final class EbDocumentTypes {

  /** Renewal advice, shared with Renewal and Customer Servicing (decision D3). */
  public static final String RENEWAL_ADVICE = "RENEWAL_ADVICE";

  /** Client feedback on the renewal advice (Marketing). */
  public static final String CLIENT_FEEDBACK = "EB_CLIENT_FEEDBACK";

  /** Signed Broker on Record letter (Marketing, Processing). */
  public static final String BOR = "EB_BOR";

  /** Terms of Reference (Marketing, Processing). */
  public static final String TOR = "EB_TOR";

  /** Named master list (Marketing, Processing). */
  public static final String MASTERLIST = "EB_MASTERLIST";

  /** Unnamed master list / census of the incumbent (Marketing). */
  public static final String MASTERLIST_UNNAMED = "EB_MASTERLIST_UNNAMED";

  /** Utilization report: health-related, Marketing only. */
  public static final String UTILIZATION = "EB_UTILIZATION";

  /** Indicative proposal of the incumbent (Marketing). */
  public static final String INDICATIVE_PROPOSAL = "EB_INDICATIVE_PROPOSAL";

  /** Insurer proposal (Marketing). */
  public static final String PROPOSAL = "EB_PROPOSAL";

  /** Comparative analysis (Marketing). */
  public static final String COMPARATIVE = "EB_COMPARATIVE";

  /** Franchise request form (Marketing). */
  public static final String FRANCHISE_FORM = "EB_FRANCHISE_FORM";

  /** Client confirmation of the chosen proposal (Marketing, Processing). */
  public static final String CLIENT_CONFIRMATION = "EB_CLIENT_CONFIRMATION";

  /** Policy form / contract (Marketing, Processing, Collection). */
  public static final String POLICY_FORM = "EB_POLICY_FORM";

  /** Insurer direct billing of member changes (Marketing, Processing, Collection). */
  public static final String DIRECT_BILLING = "EB_DIRECT_BILLING";

  /** Insurer statement of account (Processing, Collection). */
  public static final String SOA = "EB_SOA";

  /** Member change request (Marketing, Processing). */
  public static final String MEMBER_CHANGE = "EB_MEMBER_CHANGE";

  /** ISACOM approval of a non-accredited provider (Marketing; EBQ24). */
  public static final String ISACOM_APPROVAL = "EB_ISACOM_APPROVAL";

  /** Every EB document type. */
  public static final List<String> ALL =
      List.of(
          CLIENT_FEEDBACK,
          BOR,
          TOR,
          MASTERLIST,
          MASTERLIST_UNNAMED,
          UTILIZATION,
          INDICATIVE_PROPOSAL,
          PROPOSAL,
          COMPARATIVE,
          FRANCHISE_FORM,
          CLIENT_CONFIRMATION,
          POLICY_FORM,
          DIRECT_BILLING,
          SOA,
          MEMBER_CHANGE,
          ISACOM_APPROVAL);

  /** List of values of the process tags (BRID-025). */
  public static final String PROCESS_TYPE_LOV = "EB_PROCESS_TYPE";

  /** Process tag: new business placement. */
  public static final String NB_PLACEMENT = "NB_PLACEMENT";

  /** Process tag: renewal placement. */
  public static final String RENEWAL_PLACEMENT = "RENEWAL_PLACEMENT";

  /** Process tag: endorsement (member change with premium effect). */
  public static final String ENDORSEMENT = "ENDORSEMENT";

  /** Process tag: adjustment. */
  public static final String ADJUSTMENT = "ADJUSTMENT";

  /** Process tag: franchise request. */
  public static final String FRANCHISE = "FRANCHISE";

  /** Process tag: proposal. */
  public static final String PROPOSAL_PROCESS = "PROPOSAL";

  private EbDocumentTypes() {}
}
