package com.iortatechnxt.brokerverse.renewal.candidate.api.dto;

import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistory;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsement;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckRun;
import com.iortatechnxt.brokerverse.renewal.domain.Disposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignment;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverride;
import java.time.Instant;
import java.util.List;

/** Responses of the tabs of the renewal record page. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class CandidateRecordDtos {

  private CandidateRecordDtos() {}

  /**
   * The record page of a renewal (FR-RN-041).
   *
   * @param row list columns
   * @param lifecycle initiation, evaluation, proposal and links
   * @param blocking messages of the checks that block posting, the RA and acceptance
   * @param historyViewed whether the current user opened the account history
   */
  public record CandidateDetail(
      CandidateDtos.CandidateRow row,
      Lifecycle lifecycle,
      List<String> blocking,
      boolean historyViewed) {}

  /**
   * Lifecycle of a renewal.
   *
   * @param id candidate id (workflow and attachments)
   * @param policyYear policy year of the expiring invoice
   * @param initiatedBy initiating user
   * @param initiatedAt initiation time
   * @param evaluatedAt last evaluation
   * @param bucketRuleVersion bucket rule set version
   * @param proposal matrix proposal
   * @param raNotice Renewal Advice notice
   * @param path path
   * @param links renewal account, New Business path and booking
   * @param legacyPackage legacy package and the BIBS package it resolved to
   */
  public record Lifecycle(
      Long id,
      Integer policyYear,
      String initiatedBy,
      Instant initiatedAt,
      Instant evaluatedAt,
      Integer bucketRuleVersion,
      Proposal proposal,
      String raNotice,
      String path,
      Links links,
      LegacyPackage legacyPackage) {

    /**
     * Maps a candidate.
     *
     * @param c candidate
     * @return lifecycle
     */
    public static Lifecycle of(RenewalCandidate c) {
      var p = c.getProposal();
      return new Lifecycle(
          c.getId(),
          c.getPolicyYear(),
          c.getInitiatedBy(),
          c.getInitiatedAt(),
          c.getEvaluatedAt(),
          c.getBucketRuleVersion(),
          new Proposal(
              p.disposition() == null ? null : p.disposition().name(),
              p.automatic(),
              p.matrixVersion(),
              p.ruleId()),
          c.getRaNotice().name(),
          c.getPath().name(),
          new Links(
              c.getRenewalArn(),
              c.getQuotationRef(),
              c.getProposalRef(),
              c.getRenewedInvoiceNo(),
              c.getClosedAs() == null ? null : c.getClosedAs().name(),
              c.getClosedAt()),
          new LegacyPackage(
              c.getSnapshot().legacyPackageCode(),
              c.getSnapshot().legacyPackageVersion(),
              c.getResolvedProductCode(),
              c.getResolvedVersionNo()));
    }
  }

  /**
   * Proposal of the decision matrix.
   *
   * @param disposition proposed disposition
   * @param automation AUTO or MANUAL
   * @param matrixVersion matrix version
   * @param ruleId rule
   */
  public record Proposal(
      String disposition, String automation, Integer matrixVersion, Long ruleId) {}

  /**
   * Links of a renewal.
   *
   * @param renewalArn renewal account
   * @param quotationRef quotation of the New Business path
   * @param proposalRef PRF of the New Business path
   * @param renewedInvoiceNo booked renewal invoice
   * @param closedAs closure
   * @param closedAt closure time
   */
  public record Links(
      String renewalArn,
      String quotationRef,
      String proposalRef,
      String renewedInvoiceNo,
      String closedAs,
      Instant closedAt) {}

  /**
   * Package of a migrated policy.
   *
   * @param legacyCode legacy package code
   * @param legacyVersion legacy package version
   * @param productCode BIBS package
   * @param versionNo BIBS package version
   */
  public record LegacyPackage(
      String legacyCode, String legacyVersion, String productCode, Integer versionNo) {}

  /**
   * The Checks and Bucket tab (FR-RN-020, 022).
   *
   * @param results results of the latest run
   * @param runs check runs, newest first
   * @param buckets bucket changes, newest first
   * @param endorsements endorsements linked (BRRN.032)
   */
  public record ChecksView(
      List<CheckResultView> results,
      List<CheckRunView> runs,
      List<BucketChangeView> buckets,
      List<EndorsementView> endorsements) {}

  /**
   * A check result.
   *
   * @param checkCode check
   * @param checkName check name
   * @param outcome outcome
   * @param severity severity
   * @param message message
   * @param detail detail
   */
  public record CheckResultView(
      String checkCode,
      String checkName,
      String outcome,
      String severity,
      String message,
      String detail) {

    /**
     * Maps a result.
     *
     * @param r result
     * @return view
     */
    public static CheckResultView of(CheckResult r) {
      return new CheckResultView(
          r.getCheckCode(),
          CheckNames.of(r.getCheckCode()),
          r.getOutcome().name(),
          r.getSeverity().name(),
          r.getMessage(),
          r.getDetail());
    }
  }

  /**
   * A check run.
   *
   * @param id run
   * @param trigger trigger
   * @param runAt time
   * @param bucketBefore bucket before
   * @param bucketAfter bucket after
   * @param ruleSetVersion rule set version
   * @param failed failed checks
   * @param runBy user or job
   */
  public record CheckRunView(
      Long id,
      String trigger,
      Instant runAt,
      String bucketBefore,
      String bucketAfter,
      Integer ruleSetVersion,
      int failed,
      String runBy) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return view
     */
    public static CheckRunView of(CheckRun r) {
      return new CheckRunView(
          r.getId(),
          r.getTrigger().name(),
          r.getRunAt(),
          r.getBucketBefore() == null ? null : r.getBucketBefore().name(),
          r.getBucketAfter().name(),
          r.getRuleSetVersion(),
          r.getFailedCount(),
          r.getCreatedBy());
    }
  }

  /**
   * A bucket change.
   *
   * @param from from
   * @param to to
   * @param ruleSetVersion rule set version
   * @param ruleId rule
   * @param cause RULE or OVERRIDE
   * @param remarks remarks
   * @param by user
   * @param at time
   */
  public record BucketChangeView(
      String from,
      String to,
      Integer ruleSetVersion,
      Long ruleId,
      String cause,
      String remarks,
      String by,
      Instant at) {

    /**
     * Maps a change.
     *
     * @param b change
     * @return view
     */
    public static BucketChangeView of(BucketHistory b) {
      return new BucketChangeView(
          b.getFromBucket() == null ? null : b.getFromBucket().name(),
          b.getToBucket().name(),
          b.getRuleSetVersion(),
          b.getRuleId(),
          b.getCause().name(),
          b.getRemarks(),
          b.getCreatedBy(),
          b.getCreatedAt());
    }
  }

  /**
   * An endorsement linked to the renewal.
   *
   * @param reference endorsement or request number
   * @param source BOOKING or ADJUSTMENT
   * @param status status
   * @param linkedAt time linked
   */
  public record EndorsementView(String reference, String source, String status, Instant linkedAt) {

    /**
     * Maps an endorsement.
     *
     * @param e endorsement
     * @return view
     */
    public static EndorsementView of(CandidateEndorsement e) {
      return new EndorsementView(
          e.getReference(), e.getSource(), e.getStatusAtLink(), e.getCreatedAt());
    }
  }

  /**
   * The History tab (FR-RN-004): dispositions, assignments and overrides; the workflow history is
   * read from the workflow API.
   *
   * @param dispositions dispositions, newest first
   * @param assignments assignments, newest first
   * @param overrides overrides, newest first
   */
  public record HistoryViewDto(
      List<DispositionView> dispositions,
      List<AssignmentView> assignments,
      List<OverrideView> overrides) {}

  /**
   * A disposition row.
   *
   * @param code disposition
   * @param reason reason
   * @param remarks remarks
   * @param newInvoiceNo new invoice number
   * @param source source
   * @param matrixVersion matrix version
   * @param ruleId rule
   * @param superseded whether a later disposition superseded it
   * @param by user
   * @param at time
   */
  public record DispositionView(
      String code,
      String reason,
      String remarks,
      String newInvoiceNo,
      String source,
      Integer matrixVersion,
      Long ruleId,
      boolean superseded,
      String by,
      Instant at) {

    /**
     * Maps a row.
     *
     * @param d row
     * @return view
     */
    public static DispositionView of(Disposition d) {
      return new DispositionView(
          d.getCode().name(),
          d.getReasonCode(),
          d.getRemarks(),
          d.getNewInvoiceNo(),
          d.getSource().name(),
          d.getMatrixVersion(),
          d.getRuleId(),
          d.getSupersededBy() != null,
          d.getCreatedBy(),
          d.getCreatedAt());
    }
  }

  /**
   * An assignment.
   *
   * @param role AO or PO
   * @param username assignee
   * @param previous previous assignee
   * @param reason reason
   * @param by user
   * @param at time
   */
  public record AssignmentView(
      String role, String username, String previous, String reason, String by, Instant at) {

    /**
     * Maps an assignment.
     *
     * @param a assignment
     * @return view
     */
    public static AssignmentView of(RenewalAssignment a) {
      return new AssignmentView(
          a.getRole().name(),
          a.getUsername(),
          a.getPrevious(),
          a.getReasonCode(),
          a.getCreatedBy(),
          a.getCreatedAt());
    }
  }

  /**
   * An override.
   *
   * @param kind kind
   * @param checkCode check
   * @param from from
   * @param to to
   * @param reason reason
   * @param remarks remarks
   * @param active still active
   * @param by user
   * @param at time
   */
  public record OverrideView(
      String kind,
      String checkCode,
      String from,
      String to,
      String reason,
      String remarks,
      boolean active,
      String by,
      Instant at) {

    /**
     * Maps an override.
     *
     * @param o override
     * @return view
     */
    public static OverrideView of(RenewalOverride o) {
      return new OverrideView(
          o.getKind().name(),
          o.getCheckCode(),
          o.getFromValue(),
          o.getToValue(),
          o.getReasonCode(),
          o.getRemarks(),
          o.isActive(),
          o.getCreatedBy(),
          o.getCreatedAt());
    }
  }
}
