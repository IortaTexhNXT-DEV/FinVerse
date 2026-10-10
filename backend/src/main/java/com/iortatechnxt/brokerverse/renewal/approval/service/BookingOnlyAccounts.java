package com.iortatechnxt.brokerverse.renewal.approval.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateTags;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RemarkService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts For Booking Only (FRRN.014.04): the tag on one or several accounts (or by upload) with
 * the policy and official receipt numbers; before the account is submitted for placement the policy
 * number, the official receipt number, an insurer-issued policy document and the full payment are
 * required, otherwise an override approved with a justification.
 */
@Service
public class BookingOnlyAccounts {

  /** Message when the account cannot be submitted without an override. */
  public static final String NOT_READY =
      "The account is not fully paid. Override approval is required before you can proceed.";

  private static final Set<String> POLICY_DOCUMENTS = Set.of("POLICY_COPY", "EPOLICY");

  private final RenewalRecords records;
  private final RenewalBatch batch;
  private final RenewalPayments payments;
  private final DocumentService documents;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param batch one transaction per account
   * @param payments payment status
   * @param documents policy documents
   * @param currentUser user
   * @param audit audit trail
   */
  public BookingOnlyAccounts(
      RenewalRecords records,
      RenewalBatch batch,
      RenewalPayments payments,
      DocumentService documents,
      CurrentUser currentUser,
      AuditTrailService audit) {
    this.records = records;
    this.batch = batch;
    this.payments = payments;
    this.documents = documents;
    this.currentUser = currentUser;
    this.audit = audit;
  }

  /**
   * Tags or untags accounts For Booking Only, each independently.
   *
   * @param companyId company
   * @param refs renewals
   * @param tagged whether tagged
   * @return tagged and refused accounts
   */
  public BatchOutcome tag(Long companyId, List<String> refs, boolean tagged) {
    return batch.run(refs, ref -> tagOne(companyId, ref, tagged, null));
  }

  /**
   * Tags an account For Booking Only with its policy and official receipt numbers.
   *
   * @param companyId company
   * @param ref renewal
   * @param tagged whether tagged
   * @param details policy and OR numbers, may be null
   */
  @Transactional
  public void tagOne(Long companyId, String ref, boolean tagged, Details details) {
    RenewalCandidate c = records.get(companyId, ref);
    RenewalRecords.requireUnlocked(c);
    c.getPlacement().forBookingOnly(tagged);
    if (details != null) {
      c.getPlacement().getTags().bookingOnly(blank(details.policyNo()), blank(details.orNo()));
    }
    if (!tagged) {
      c.getPlacement().getTags().override(null, null, null);
    }
    audit.record(
        RenewalCodes.ENTITY,
        ref,
        AuditAction.UPDATE,
        tagged ? "Tagged For Booking Only" : "For Booking Only tag removed");
  }

  /**
   * What is missing before an account For Booking Only can be submitted for placement.
   *
   * @param c renewal
   * @return missing items, empty when ready or overridden or not tagged
   */
  @Transactional(readOnly = true)
  public List<String> missing(RenewalCandidate c) {
    List<String> missing = new ArrayList<>();
    if (!c.getPlacement().isForBookingOnly()
        || CandidateTags.APPROVED.equals(c.getPlacement().getTags().getOverride())) {
      return missing;
    }
    if (c.getPlacement().getTags().getPolicyNo() == null
        && c.getPlacement().getEpolicyNo() == null) {
      missing.add("Policy Number");
    }
    if (c.getPlacement().getTags().getOrNo() == null) {
      missing.add("OR Number");
    }
    boolean document =
        documents.list(new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString())).stream()
            .anyMatch(a -> POLICY_DOCUMENTS.contains(a.getDocumentType()));
    if (!document) {
      missing.add("insurer-issued policy document");
    }
    if (!RenewalPayments.PAID.equals(payments.of(c).status())) {
      missing.add("full payment");
    }
    return missing;
  }

  /**
   * Refuses the submission of an account For Booking Only that is not ready.
   *
   * @param c renewal
   */
  public void requireReady(RenewalCandidate c) {
    if (!missing(c).isEmpty()) {
      throw new BusinessRuleException("RNW_BOOKING_ONLY_OVERRIDE", NOT_READY);
    }
  }

  /**
   * Requests the override of an account For Booking Only.
   *
   * @param companyId company
   * @param ref renewal
   * @param reason why
   */
  @Transactional
  public void requestOverride(Long companyId, String ref, String reason) {
    RenewalCandidate c = records.get(companyId, ref);
    if (!c.getPlacement().isForBookingOnly()) {
      throw new BusinessRuleException("RNW_BOOKING_ONLY", "The account is not For Booking Only");
    }
    String why = RemarkService.requireText(reason, "Enter the reason of the override");
    c.getPlacement().getTags().override(CandidateTags.REQUESTED, currentUser.username(), why);
    audit.record(RenewalCodes.ENTITY, ref, AuditAction.SUBMIT, "Override requested: " + why);
  }

  /**
   * Approves or rejects the override; the approver gives a justification.
   *
   * @param companyId company
   * @param ref renewal
   * @param approve approve or reject
   * @param justification justification
   */
  @Transactional
  public void decideOverride(Long companyId, String ref, boolean approve, String justification) {
    RenewalCandidate c = records.get(companyId, ref);
    if (!CandidateTags.REQUESTED.equals(c.getPlacement().getTags().getOverride())) {
      throw new BusinessRuleException("RNW_BOOKING_ONLY", "No override is requested");
    }
    String why = RemarkService.requireText(justification, "Enter the override justification");
    if (currentUser.username().equals(c.getPlacement().getTags().getOverrideBy())) {
      throw new BusinessRuleException(
          "RNW_BOOKING_ONLY_SAME_USER", "The override is approved by another user");
    }
    c.getPlacement()
        .getTags()
        .override(
            approve ? CandidateTags.APPROVED : CandidateTags.REJECTED, currentUser.username(), why);
    audit.record(
        RenewalCodes.ENTITY,
        ref,
        approve ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        "Override " + (approve ? "approved: " : "rejected: ") + why);
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /**
   * The policy and official receipt numbers of an account For Booking Only.
   *
   * @param policyNo policy number
   * @param orNo official receipt number
   */
  public record Details(String policyNo, String orNo) {}
}
