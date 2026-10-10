package com.iortatechnxt.brokerverse.renewal.approval.api;

import com.iortatechnxt.brokerverse.renewal.approval.service.BookingOnlyAccounts;
import com.iortatechnxt.brokerverse.renewal.approval.service.ClientDecisions;
import com.iortatechnxt.brokerverse.renewal.approval.service.DirectToInsurerTags;
import com.iortatechnxt.brokerverse.renewal.approval.service.RenewalApprovals;
import com.iortatechnxt.brokerverse.renewal.approval.service.RenewalPayments;
import com.iortatechnxt.brokerverse.renewal.approval.service.SubmissionGate;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateDecision;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateTags;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The client's answer, the second approval and the tags of a renewal account (FRRN.014.04,
 * FRRN.014.05, FRRN.019.01, FRRN.025.01, FRRN.26.01, FRRN.028.01).
 */
@RestController
@RequestMapping("/api/v1/renewal")
public class RenewalApprovalController {

  private static final String VIEW = "hasAuthority('RNW_VIEW')";
  private static final String MARKETING =
      "hasAnyAuthority('RNW_DISPOSE','RNW_ACCEPT','RNW_PROCESS')";
  private static final String APPROVER = "hasAnyAuthority('RNW_REVIEW','RNW_PROCESS_ASSIGN')";

  private final RenewalRecords records;
  private final ClientDecisions decisions;
  private final RenewalApprovals approvals;
  private final SubmissionGate gate;
  private final BookingOnlyAccounts bookingOnly;
  private final DirectToInsurerTags directToInsurer;
  private final RenewalPayments payments;

  /**
   * Creates the controller.
   *
   * @param records renewals
   * @param decisions client acceptance status
   * @param approvals second approval
   * @param gate submission checks
   * @param bookingOnly For Booking Only
   * @param directToInsurer Direct-to-Insurer Payment
   * @param payments payment status
   */
  public RenewalApprovalController(
      RenewalRecords records,
      ClientDecisions decisions,
      RenewalApprovals approvals,
      SubmissionGate gate,
      BookingOnlyAccounts bookingOnly,
      DirectToInsurerTags directToInsurer,
      RenewalPayments payments) {
    this.records = records;
    this.decisions = decisions;
    this.approvals = approvals;
    this.gate = gate;
    this.bookingOnly = bookingOnly;
    this.directToInsurer = directToInsurer;
    this.payments = payments;
  }

  /**
   * The answer, approval, tags and payment status of an account.
   *
   * @param companyId company
   * @param ref renewal
   * @return state
   */
  @GetMapping("/candidates/{ref}/approval")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public State state(@RequestParam Long companyId, @PathVariable String ref) {
    RenewalCandidate c = records.get(companyId, ref);
    CandidateDecision d = c.getPlacement().getDecision();
    CandidateTags t = c.getPlacement().getTags();
    RenewalPayments.Status p = payments.of(c);
    return new State(
        new Client(d.getClientStatus(), d.getClientRemarks(), d.getClientBy(), d.getClientAt()),
        new Approval(
            gate.secondApproval(),
            d.getApprovalStatus(),
            d.getApprovalRemarks(),
            d.getApprovalBy(),
            d.getApprovalAt(),
            d.getPaymentConfirmation()),
        new BookingOnly(
            c.getPlacement().isForBookingOnly(),
            t.getPolicyNo(),
            t.getOrNo(),
            t.getOverride(),
            t.getOverrideReason(),
            bookingOnly.missing(c)),
        new DirectToInsurer(
            t.getDtiOption(), t.getDtiStatus(), t.getDtiDecidedBy(), t.getDtiReason()),
        new Payment(p.status(), p.premium(), p.paid(), p.outstanding()),
        t.isAmortized());
  }

  /**
   * Records the client's answer.
   *
   * @param companyId company
   * @param ref renewal
   * @param body status and remarks
   */
  @PutMapping("/candidates/{ref}/client-status")
  @PreAuthorize(MARKETING)
  public void clientStatus(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody ClientBody body) {
    decisions.record(companyId, ref, body.status(), body.remarks());
  }

  /**
   * Approves accounts Submitted for Approval.
   *
   * @param companyId company
   * @param body accounts and the client's payment confirmation
   * @return approved and refused
   */
  @PostMapping("/approvals/approve")
  @PreAuthorize(APPROVER)
  public BatchOutcome approve(@RequestParam Long companyId, @RequestBody ApproveBody body) {
    return approvals.approve(companyId, body.refs(), body.paymentConfirmation());
  }

  /**
   * Returns accounts Submitted for Approval for correction.
   *
   * @param companyId company
   * @param body accounts and remarks
   * @return returned and refused
   */
  @PostMapping("/approvals/reject")
  @PreAuthorize(APPROVER)
  public BatchOutcome reject(@RequestParam Long companyId, @RequestBody RejectBody body) {
    return approvals.reject(companyId, body.refs(), body.remarks());
  }

  /**
   * Resubmits a returned account.
   *
   * @param companyId company
   * @param ref renewal
   */
  @PostMapping("/candidates/{ref}/resubmit")
  @PreAuthorize(MARKETING)
  public void resubmit(@RequestParam Long companyId, @PathVariable String ref) {
    approvals.resubmit(companyId, ref);
  }

  /**
   * Submits accepted accounts for placement once their tags are cleared.
   *
   * @param companyId company
   * @param body accounts
   * @return submitted and refused
   */
  @PostMapping("/approvals/submit-placement")
  @PreAuthorize(MARKETING)
  public BatchOutcome submitForPlacement(@RequestParam Long companyId, @RequestBody Refs body) {
    return approvals.submitForPlacement(companyId, body.refs());
  }

  /**
   * Tags accounts For Booking Only, or removes the tag.
   *
   * @param companyId company
   * @param body accounts and the tag
   * @return tagged and refused
   */
  @PostMapping("/booking-only")
  @PreAuthorize(MARKETING)
  public BatchOutcome tagBookingOnly(@RequestParam Long companyId, @RequestBody TagBody body) {
    return bookingOnly.tag(companyId, body.refs(), body.tagged());
  }

  /**
   * Records the policy and official receipt numbers of an account For Booking Only.
   *
   * @param companyId company
   * @param ref renewal
   * @param body numbers
   */
  @PutMapping("/candidates/{ref}/booking-only")
  @PreAuthorize(MARKETING)
  public void bookingOnlyDetails(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody DetailsBody body) {
    bookingOnly.tagOne(
        companyId,
        ref,
        body.tagged(),
        new BookingOnlyAccounts.Details(body.policyNo(), body.orNo()));
  }

  /**
   * Requests the override of an account For Booking Only.
   *
   * @param companyId company
   * @param ref renewal
   * @param body reason
   */
  @PostMapping("/candidates/{ref}/booking-only/override")
  @PreAuthorize(MARKETING)
  public void requestOverride(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Remarks body) {
    bookingOnly.requestOverride(companyId, ref, body.remarks());
  }

  /**
   * Decides the override of an account For Booking Only.
   *
   * @param companyId company
   * @param ref renewal
   * @param body decision and justification
   */
  @PostMapping("/candidates/{ref}/booking-only/override/decision")
  @PreAuthorize("hasAuthority('RNW_OVERRIDE')")
  public void decideOverride(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Decision body) {
    bookingOnly.decideOverride(companyId, ref, body.approve(), body.remarks());
  }

  /**
   * Tags an account Direct-to-Insurer Payment, or removes the tag.
   *
   * @param companyId company
   * @param ref renewal
   * @param body option
   */
  @PutMapping("/candidates/{ref}/direct-to-insurer")
  @PreAuthorize(MARKETING)
  public void tagDirectToInsurer(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody DtiBody body) {
    directToInsurer.tag(companyId, ref, body.option());
  }

  /**
   * The Unit Head's decision on a Direct-to-Insurer Payment.
   *
   * @param companyId company
   * @param ref renewal
   * @param body decision and reason
   */
  @PostMapping("/candidates/{ref}/direct-to-insurer/decision")
  @PreAuthorize("hasAuthority('RNW_REVIEW')")
  public void decideDirectToInsurer(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Decision body) {
    directToInsurer.decide(companyId, ref, body.approve(), body.remarks());
  }

  /**
   * Accounts.
   *
   * @param refs references
   */
  public record Refs(List<String> refs) {}

  /**
   * The client's answer.
   *
   * @param status status
   * @param remarks remarks
   */
  public record ClientBody(String status, String remarks) {}

  /**
   * An approval.
   *
   * @param refs accounts
   * @param paymentConfirmation the client's payment confirmation
   */
  public record ApproveBody(List<String> refs, String paymentConfirmation) {}

  /**
   * A return for correction.
   *
   * @param refs accounts
   * @param remarks reason
   */
  public record RejectBody(List<String> refs, String remarks) {}

  /**
   * A tag on accounts.
   *
   * @param refs accounts
   * @param tagged whether tagged
   */
  public record TagBody(List<String> refs, boolean tagged) {}

  /**
   * The details of an account For Booking Only.
   *
   * @param tagged whether tagged
   * @param policyNo policy number
   * @param orNo official receipt number
   */
  public record DetailsBody(boolean tagged, String policyNo, String orNo) {}

  /**
   * Remarks.
   *
   * @param remarks text
   */
  public record Remarks(String remarks) {}

  /**
   * A decision.
   *
   * @param approve approve or reject
   * @param remarks justification or reason
   */
  public record Decision(boolean approve, String remarks) {}

  /**
   * The Direct-to-Insurer Payment option.
   *
   * @param option BLANKET, UNIT_HEAD or null to remove
   */
  public record DtiBody(String option) {}

  /**
   * The state of an account.
   *
   * @param client client acceptance status
   * @param approval second approval
   * @param bookingOnly For Booking Only
   * @param directToInsurer Direct-to-Insurer Payment
   * @param payment payment status
   * @param amortized amortized premium
   */
  public record State(
      Client client,
      Approval approval,
      BookingOnly bookingOnly,
      DirectToInsurer directToInsurer,
      Payment payment,
      boolean amortized) {}

  /**
   * The client's answer.
   *
   * @param status status
   * @param remarks remarks
   * @param by user
   * @param at time
   */
  public record Client(String status, String remarks, String by, Instant at) {}

  /**
   * The second approval.
   *
   * @param required whether the second approval is on
   * @param status status
   * @param remarks remarks
   * @param by user
   * @param at time
   * @param paymentConfirmation the client's payment confirmation
   */
  public record Approval(
      boolean required,
      String status,
      String remarks,
      String by,
      Instant at,
      String paymentConfirmation) {}

  /**
   * For Booking Only.
   *
   * @param tagged tag
   * @param policyNo policy number
   * @param orNo official receipt number
   * @param override override status
   * @param overrideReason override reason or justification
   * @param missing what is missing before the submission
   */
  public record BookingOnly(
      boolean tagged,
      String policyNo,
      String orNo,
      String override,
      String overrideReason,
      List<String> missing) {}

  /**
   * Direct-to-Insurer Payment.
   *
   * @param option option
   * @param status Unit Head approval status
   * @param decidedBy Unit Head
   * @param reason rejection reason
   */
  public record DirectToInsurer(String option, String status, String decidedBy, String reason) {}

  /**
   * The payment status.
   *
   * @param status PAID, PARTIALLY_PAID or UNPAID
   * @param premium total premium
   * @param paid paid
   * @param outstanding outstanding balance
   */
  public record Payment(
      String status, BigDecimal premium, BigDecimal paid, BigDecimal outstanding) {}
}
