package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.report.core.CodeSet;
import java.time.LocalDate;

/**
 * Criteria of a renewal list (FR-RN-011, 012, 040, 062): the tab, the expiry range, the search and
 * the multi-select criteria of the Filters panel, each an include or "all except" list (BRRN.003).
 *
 * @param companyId company
 * @param tab list tab
 * @param search search text (reference, ARN, invoice, policy, PN, client code or name)
 * @param expiryFrom first expiry, may be null
 * @param expiryTo last expiry, may be null
 * @param codes multi-select criteria
 * @param flags quick filters
 */
public record CandidateFilter(
    Long companyId,
    Tab tab,
    String search,
    LocalDate expiryFrom,
    LocalDate expiryTo,
    Codes codes,
    Flags flags) {

  /**
   * Multi-select criteria (BRD 1.003.3.1.1-13).
   *
   * @param unitHead unit head
   * @param origin business origin
   * @param accountType account type
   * @param region region
   * @param department department (area)
   * @param branch invoicing branch
   * @param riskCode risk code
   * @param segment market segment
   * @param officer account officer
   * @param bucket bucket
   * @param disposition disposition
   * @param stage stage
   * @param insurer insurer
   */
  public record Codes(
      CodeSet unitHead,
      CodeSet origin,
      CodeSet accountType,
      CodeSet region,
      CodeSet department,
      CodeSet branch,
      CodeSet riskCode,
      CodeSet segment,
      CodeSet officer,
      CodeSet bucket,
      CodeSet disposition,
      CodeSet stage,
      CodeSet insurer) {

    /** No selection. */
    public static final Codes NONE =
        new Codes(
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL,
            CodeSet.ALL);
  }

  /**
   * Quick filters.
   *
   * @param assignedToMe renewals ever assigned to the user as AO (My Dispositions)
   * @param assignedPo Processing Officer, may be null
   * @param returned returned only
   * @param nrns NRNS only
   * @param urgent urgent only
   * @param kycDue KYC due only
   * @param dueWithinDays expiring within the days, may be null
   * @param disposed dispositioned only
   */
  public record Flags(
      boolean assignedToMe,
      String assignedPo,
      boolean returned,
      boolean nrns,
      boolean urgent,
      boolean kycDue,
      Integer dueWithinDays,
      boolean disposed) {

    /** No quick filter. */
    public static final Flags NONE =
        new Flags(false, null, false, false, false, false, null, false);
  }

  /** Tabs of the renewal lists (Expiry List, Processing Worklist, Letters). */
  public enum Tab {
    /** Every renewal. */
    ALL,
    /** Extracted, not initiated. */
    EXTRACTED,
    /** Unassigned Disposition. */
    UNASSIGNED,
    /** Disposition For Renewal. */
    FOR_RENEWAL,
    /** Disposition For Quotation. */
    FOR_QUOTATION,
    /** Disposition For Proposal. */
    FOR_PROPOSAL,
    /** Disposition Not for Renewal. */
    NOT_FOR_RENEWAL,
    /** Disposition Lost Business. */
    LOST_BUSINESS,
    /** Exception bucket. */
    EXCEPTIONS,
    /** Team Leader review. */
    REVIEW,
    /** Transfer pending. */
    TRANSFER_PENDING,
    /** Processing: for processing. */
    FOR_PROCESSING,
    /** Processing: in processing. */
    IN_PROCESSING,
    /** Processing: with insurer. */
    WITH_INSURER,
    /** Processing: insurer responded (RA ready). */
    INSURER_RESPONDED,
    /** Processing: returned to Marketing. */
    RETURNED,
    /** Letters: RA ready. */
    RA_READY,
    /** Letters: RA generated. */
    RA_GENERATED,
    /** Letters: RA sent. */
    RA_SENT,
    /** Letters: letter pending (NAL, NFR). */
    LETTER_PENDING,
    /** Letters: unrenewed at the effective expiry, No Advice Letter due (Operations). */
    NAL_DUE,
    /** Letters: unrenewed at the effective expiry, Non-Renewal Letter due (Marketing AO). */
    NRL_DUE,
    /** Letters: NRNS. */
    NRNS,
    /** Renewals with an attention flag (ageing, overdue, high risk; FR-RN-102). */
    ATTENTION,
    /** New Business path. */
    NB_PATH,
    /** Closed and renewed. */
    CLOSED,
    /** Bucket panel Clean (FRRN.002.05): Clean, not Not for Renewal. */
    BUCKET_CLEAN,
    /** Bucket panel Review: Review (and Exception when the third bucket is Non-Renewable). */
    BUCKET_REVIEW,
    /** Bucket panel Review of the Clean / Review / Exception setting: Review only. */
    BUCKET_REVIEW_ONLY,
    /** Bucket panel Non-Renewable: disposition Not for Renewal. */
    BUCKET_NON_RENEWABLE
  }

  /**
   * The same criteria on another tab.
   *
   * @param other tab
   * @return criteria
   */
  public CandidateFilter withTab(Tab other) {
    return new CandidateFilter(companyId, other, search, expiryFrom, expiryTo, codes, flags);
  }
}
