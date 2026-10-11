package com.iortatechnxt.brokerverse.brokerclaims.domain;

/**
 * Shared codes of the broking claims module seeded by V1020 (CLAIMS_BROKING_DESIGN 4, 8 and 9):
 * entity type, workflow, lists of values, attribute names, parameters, alerts, notification events,
 * template, document number prefixes and the retention record type. The waves use these constants
 * instead of repeating the literals.
 */
public final class ClaimCodes {

  /** Entity type of attachments, workflow cases, audit entries and document names. */
  public static final String ENTITY_TYPE = "BrokerClaim";

  /** Workflow of a claim; its stage codes are the {@link ClaimPhase} names. */
  public static final String WORKFLOW = "BCL_CLAIM";

  /** Record type of the retention rule (nbadmin, NFR p.41). */
  public static final String RETENTION_RECORD_TYPE = "BROKER_CLAIM";

  /** Document number prefix of a claim: {@code BCL-<yyyy>-nnnnnn} (CLQ13). */
  public static final String CLAIM_NO_PREFIX = "BCL";

  /** Document number prefix of a claims authorization code: {@code CAC-<yyyy>-nnnnnn}. */
  public static final String AUTHORIZATION_CODE_PREFIX = "CAC";

  /** Module of the Claims exception codes and notification events. */
  public static final String MODULE = "CLAIMS_HANDLING";

  /** Loss advice template (docgen), e-mailed to the insurer (BRCLM.041). */
  public static final String LOSS_ADVICE_TEMPLATE = "BCL_LOSS_ADVICE";

  /** Claim statuses; attributes in {@code bcl_lov_attribute}. */
  public static final String LOV_STATUS = "BCL_CLAIM_STATUS";

  /** Requested types of settlement; attributes in {@code bcl_lov_attribute}. */
  public static final String LOV_SETTLEMENT_TYPE = "BCL_SETTLEMENT_TYPE";

  /** Adjusters / appraisers. */
  public static final String LOV_ADJUSTER = "BCL_ADJUSTER";

  /** Catastrophe codes. */
  public static final String LOV_CATASTROPHE = "BCL_CATASTROPHE";

  /** Nature of loss. */
  public static final String LOV_LOSS_NATURE = "BCL_LOSS_NATURE";

  /** Claim types. */
  public static final String LOV_CLAIM_TYPE = "BCL_CLAIM_TYPE";

  /** Claims units (status access matrix, handler register). */
  public static final String LOV_UNIT = "BCL_UNIT";

  /** Sources of insurer updates. */
  public static final String LOV_UPDATE_SOURCE = "BCL_UPDATE_SOURCE";

  /** Diary entry types. */
  public static final String LOV_DIARY_TYPE = "BCL_DIARY_TYPE";

  /** Claim document types (parent = platform DOCUMENT_TYPE). */
  public static final String LOV_DOCUMENT_TYPE = "BCL_DOCUMENT_TYPE";

  /** Reopen reasons (reason list of transition {@code reopen}). */
  public static final String LOV_REOPEN_REASON = "BCL_REOPEN_REASON";

  /** Override reasons (claimant, follow-up date, reported date). */
  public static final String LOV_OVERRIDE_REASON = "BCL_OVERRIDE_REASON";

  /** Status attribute: phase of the status ({@link ClaimPhase} name). */
  public static final String ATTR_PHASE = "phase";

  /** Status attribute: party the claim waits on. */
  public static final String ATTR_WAITING_ON = "waiting_on";

  /** Status attribute: days to the next follow-up (else {@link #PARAM_FOLLOW_UP_DAYS}). */
  public static final String ATTR_FOLLOW_UP_DAYS = "follow_up_days";

  /** Status attribute: the claim feeds the claims special remittance ({@code true}). */
  public static final String ATTR_AWAITING_PREMIUM_REMITTANCE = "awaiting_premium_remittance";

  /** Settlement type attribute: SETTLED or CLOSED_WITHOUT_PAYMENT. */
  public static final String ATTR_OUTCOME = "outcome";

  /** Settlement type attribute: the type closes the claim permanently ({@code true}). */
  public static final String ATTR_CLOSES_CLAIM = "closes_claim";

  /** Settlement type attribute: amount and date are required ({@code true}). */
  public static final String ATTR_REQUIRES_SETTLEMENT_AMOUNT = "requires_settlement_amount";

  /** Parameter: default claim currency. */
  public static final String PARAM_DEFAULT_CURRENCY = "BCL_DEFAULT_CURRENCY";

  /** Parameter: default days to the next follow-up. */
  public static final String PARAM_FOLLOW_UP_DAYS = "BCL_FOLLOW_UP_DAYS";

  /** Parameter: ageing bucket upper bounds. */
  public static final String PARAM_AGEING_BUCKETS = "BCL_AGEING_BUCKETS";

  /** Parameter: past due threshold in days. */
  public static final String PARAM_PAST_DUE_DAYS = "BCL_PAST_DUE_DAYS";

  /** Parameter: claims that make a location claims-prone. */
  public static final String PARAM_PRONE_MIN_CLAIMS = "BCL_PRONE_MIN_CLAIMS";

  /** Parameter: look-back years of the claims-prone analysis. */
  public static final String PARAM_PRONE_YEARS = "BCL_PRONE_YEARS";

  /** Parameter: authorization code on direct-payment covers (ALLOW / CONFIRM / BLOCK). */
  public static final String PARAM_AUTH_DP_POLICY = "BCL_AUTH_DP_POLICY";

  private ClaimCodes() {}
}
