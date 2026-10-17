package com.iortatechnxt.brokerverse.nonpackage.domain;

import java.util.EnumSet;
import java.util.Set;

/** Status of a PRF, mirrored from its NB_PROPOSAL work case (BRNB.022). */
public enum ProposalStatus {
  /** Being prepared by the Account Officer. */
  DRAFT,
  /** Waiting for Marketing approval. */
  FOR_MKT_APPROVAL,
  /** In the TSU queue. */
  WITH_TSU,
  /** TSU prepares the quotation slip. */
  QS_PREPARATION,
  /** Quotation slip waiting for TSU approval. */
  QS_FOR_APPROVAL,
  /** Quotation slip sent; waiting for the insurers' terms. */
  QS_SENT,
  /** Insurer terms complete. */
  TERMS_RECEIVED,
  /** Proposal slip waiting for TSU approval. */
  PS_FOR_APPROVAL,
  /** Proposal slip released to Marketing. */
  PS_RELEASED,
  /** Proposal sent to the client. */
  SENT_TO_CLIENT,
  /** Accepted by the client. */
  ACCEPTED,
  /** Accounts created. */
  CONVERTED,
  /** Declined by the client. */
  NOT_PROCEEDED,
  /** Voided while in process. */
  VOIDED;

  /** Stages in which Marketing may still change the PRF. */
  public static final Set<ProposalStatus> MARKETING_EDITABLE = EnumSet.of(DRAFT);

  /** Stages in which TSU may update the PRF (BRNB.007). */
  public static final Set<ProposalStatus> TSU_EDITABLE = EnumSet.of(WITH_TSU, QS_PREPARATION);

  /** Stages in which insurer responses may be keyed in (BRNB.009). */
  public static final Set<ProposalStatus> RESPONSES_OPEN = EnumSet.of(QS_SENT, TERMS_RECEIVED);
}
