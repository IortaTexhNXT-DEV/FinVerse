package com.iortatechnxt.brokerverse.submitted.review.api.dto;

import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovable;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaaf;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafLink;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafReview;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSignature;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Records of the review, IAAF and TOR API (FRS FR-SP-040, 041, 051-053). */
public final class ReviewDtos {

  private ReviewDtos() {}

  /**
   * A review.
   *
   * @param id id
   * @param reviewNo number
   * @param reviewDate date
   * @param reviewer reviewer
   * @param adequacy ADEQUATE or WITH_FINDINGS
   * @param findings finding codes
   * @param remarks remarks
   * @param sentTo bank counterpart the findings went to
   * @param sentAt time
   */
  public record ReviewView(
      Long id,
      int reviewNo,
      LocalDate reviewDate,
      String reviewer,
      String adequacy,
      List<String> findings,
      String remarks,
      String sentTo,
      Instant sentAt) {

    /**
     * Maps a review.
     *
     * @param r review
     * @return view
     */
    public static ReviewView from(SbmIaafReview r) {
      return new ReviewView(
          r.getId(),
          r.getReviewNo(),
          r.getReviewDate(),
          r.getReviewer(),
          r.getAdequacy(),
          r.findingList(),
          r.getRemarks(),
          r.getSentTo(),
          r.getSentAt());
    }
  }

  /**
   * A new review.
   *
   * @param reviewDate date
   * @param adequacy ADEQUATE or WITH_FINDINGS
   * @param findings finding codes
   * @param remarks remarks
   */
  public record ReviewRequest(
      @NotNull LocalDate reviewDate,
      @NotBlank String adequacy,
      List<String> findings,
      String remarks) {

    /**
     * The content.
     *
     * @return content
     */
    public SbmIaafReview.Content content() {
      return new SbmIaafReview.Content(reviewDate, adequacy, findings, remarks);
    }
  }

  /**
   * The approval state of an IAAF or TOR.
   *
   * @param status status
   * @param currentLevel level waiting
   * @param totalLevels levels
   * @param submittedAt submission
   * @param preparedBy preparer
   * @param preparedAt creation
   * @param returnReason reason of the last return
   * @param attachmentId signed PDF
   * @param levels levels of the matrix band
   * @param signatures signatures
   */
  public record Approval(
      String status,
      int currentLevel,
      int totalLevels,
      Instant submittedAt,
      String preparedBy,
      Instant preparedAt,
      String returnReason,
      Long attachmentId,
      List<LevelView> levels,
      List<SignatureView> signatures) {

    /**
     * Maps the approval state of a document.
     *
     * @param d document
     * @param levels levels
     * @param signatures signatures
     * @return state
     */
    public static Approval of(
        SbmApprovable d, List<SbmApprovalMatrix> levels, List<SbmSignature> signatures) {
      return new Approval(
          d.getStatus().name(),
          d.getCurrentLevel(),
          d.getTotalLevels(),
          d.getSubmittedAt(),
          d.getCreatedBy(),
          d.getCreatedAt(),
          d.getReturnReason(),
          d.getAttachmentId(),
          levels.stream().map(LevelView::from).toList(),
          signatures.stream().map(SignatureView::from).toList());
    }
  }

  /**
   * A level of the matrix.
   *
   * @param level level
   * @param permission permission of the approvers
   * @param approverUsername named approver
   * @param signatoryTitle title
   */
  public record LevelView(
      int level, String permission, String approverUsername, String signatoryTitle) {

    /**
     * Maps a level.
     *
     * @param m level
     * @return view
     */
    public static LevelView from(SbmApprovalMatrix m) {
      return new LevelView(
          m.getLevel(), m.getPermission(), m.getApproverUsername(), m.getSignatoryTitle());
    }
  }

  /**
   * A signature.
   *
   * @param level level
   * @param signer login
   * @param signerName name
   * @param position position
   * @param signedAt time
   * @param method STAMPED or ESIG
   * @param hash hash
   */
  public record SignatureView(
      int level,
      String signer,
      String signerName,
      String position,
      Instant signedAt,
      String method,
      String hash) {

    /**
     * Maps a signature.
     *
     * @param s signature
     * @return view
     */
    public static SignatureView from(SbmSignature s) {
      return new SignatureView(
          s.getLevel(),
          s.getSigner(),
          s.getSignerName(),
          s.getPosition(),
          s.getSignedAt(),
          s.getMethod(),
          s.getHash());
    }
  }

  /**
   * An IAAF.
   *
   * @param id id
   * @param iaafNo number
   * @param policyId policy
   * @param sbmNo masterlist number
   * @param assuredName assured
   * @param segment segment
   * @param sumInsured sum insured
   * @param sentTo bank counterpart
   * @param sentAt time
   * @param approval approval state
   * @param reviews reviews
   * @param links related policies
   */
  public record IaafView(
      Long id,
      String iaafNo,
      Long policyId,
      String sbmNo,
      String assuredName,
      String segment,
      BigDecimal sumInsured,
      String sentTo,
      Instant sentAt,
      Approval approval,
      List<ReviewView> reviews,
      List<LinkView> links) {

    /**
     * Maps an IAAF.
     *
     * @param i IAAF
     * @param policy masterlist number, assured, segment and sum insured
     * @param approval approval state
     * @param reviews reviews
     * @param links related policies
     * @return view
     */
    public static IaafView from(
        SbmIaaf i,
        PolicyFacts policy,
        Approval approval,
        List<ReviewView> reviews,
        List<LinkView> links) {
      return new IaafView(
          i.getId(),
          i.getIaafNo(),
          i.getPolicyId(),
          policy.sbmNo(),
          policy.assuredName(),
          policy.segment(),
          policy.sumInsured(),
          i.getSentTo(),
          i.getSentAt(),
          approval,
          reviews,
          links);
    }
  }

  /**
   * The policy facts shown with a document.
   *
   * @param sbmNo masterlist number
   * @param assuredName assured
   * @param segment segment
   * @param sumInsured sum insured
   */
  public record PolicyFacts(
      String sbmNo, String assuredName, String segment, BigDecimal sumInsured) {}

  /**
   * A related policy of an IAAF.
   *
   * @param policyId related record
   * @param sbmNo its masterlist number
   * @param relation relation
   */
  public record LinkView(Long policyId, String sbmNo, String relation) {

    /**
     * Maps a link.
     *
     * @param l link
     * @param sbmNo masterlist number
     * @return view
     */
    public static LinkView from(SbmIaafLink l, String sbmNo) {
      return new LinkView(l.getRelatedPolicyId(), sbmNo, l.getRelation());
    }
  }

  /**
   * A new IAAF.
   *
   * @param related related policies by id and their relation
   */
  public record IaafRequest(Map<Long, String> related) {}

  /**
   * A TOR.
   *
   * @param id id
   * @param torNo number
   * @param policyId policy
   * @param arn renewal account
   * @param sbmNo masterlist number
   * @param assuredName assured
   * @param breaches breached limits
   * @param proposedTerms proposed terms
   * @param aoUsername Account Officer
   * @param approvedAt approval
   * @param releasedAt release
   * @param approval approval state
   */
  public record TorView(
      Long id,
      String torNo,
      Long policyId,
      String arn,
      String sbmNo,
      String assuredName,
      String breaches,
      String proposedTerms,
      String aoUsername,
      Instant approvedAt,
      Instant releasedAt,
      Approval approval) {

    /**
     * Maps a TOR.
     *
     * @param t TOR
     * @param policy masterlist number and assured
     * @param approval approval state
     * @return view
     */
    public static TorView from(SbmTor t, PolicyFacts policy, Approval approval) {
      return new TorView(
          t.getId(),
          t.getTorNo(),
          t.getPolicyId(),
          t.getArn(),
          policy.sbmNo(),
          policy.assuredName(),
          t.getBreaches(),
          t.getProposedTerms(),
          t.getAoUsername(),
          t.getApprovedAt(),
          t.getReleasedAt(),
          approval);
    }
  }

  /**
   * A new or changed TOR.
   *
   * @param policyId flagged record (new TOR)
   * @param proposedTerms proposed terms
   * @param aoUsername Account Officer
   */
  public record TorRequest(
      Long policyId, @NotBlank String proposedTerms, @NotBlank String aoUsername) {}

  /**
   * A return or cancellation.
   *
   * @param reasonCode reason (LOV SBM_RETURN_REASON)
   * @param comment comment
   */
  public record ReturnRequest(String reasonCode, String comment) {}
}
