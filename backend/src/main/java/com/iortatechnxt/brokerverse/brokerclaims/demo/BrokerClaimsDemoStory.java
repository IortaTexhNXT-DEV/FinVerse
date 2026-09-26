package com.iortatechnxt.brokerverse.brokerclaims.demo;

import com.iortatechnxt.brokerverse.brokerclaims.diary.domain.DiaryEntry;
import com.iortatechnxt.brokerverse.brokerclaims.diary.service.DiaryService;
import com.iortatechnxt.brokerverse.brokerclaims.diary.service.DiaryService.DiaryInput;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimClosureService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimClosureService.Settlement;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimFollowUpService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The claim-level demo storyline of Claims Handling (wave CL2): what the handlers did on the demo
 * claims after recording them, through the real status, closure, follow-up and diary services as
 * the demo users of the role / unit matrix - documents completed, an adjuster appointed, an action
 * plan, diary calls, meetings and follow-ups, a follow-up date overridden, a temporary closure, a
 * claim settled on the insurer's LOA and closed, a claim closed within the deductible and reopened
 * with a reason, and a claim waiting for the premium remittance. Called by {@link
 * BrokerClaimsDemoData}.
 */
@Component
@Profile("demo")
public class BrokerClaimsDemoStory {

  static final String OFFICER = "clmofficer";
  static final String NON_MOTOR = "clmofficer2";
  static final String TL = "clmtl";
  static final String TH = "clmth";
  private static final int MEETING_IN_DAYS = 1;
  private static final int FOLLOW_UP_IN_DAYS = 2;
  private static final int OVERRIDE_IN_DAYS = 3;

  private final ClaimStatusService statuses;
  private final ClaimClosureService closures;
  private final ClaimFollowUpService followUps;
  private final DiaryService diaries;
  private final DemoUsers users;
  private final Clock clock;

  /**
   * Creates the storyline.
   *
   * @param statuses status changes
   * @param closures settlement, closure and reopen
   * @param followUps follow-up, action plan and adjuster
   * @param diaries diary entries
   * @param users demo sign-in
   * @param clock clock
   */
  public BrokerClaimsDemoStory(
      ClaimStatusService statuses,
      ClaimClosureService closures,
      ClaimFollowUpService followUps,
      DiaryService diaries,
      DemoUsers users,
      Clock clock) {
    this.statuses = statuses;
    this.closures = closures;
    this.followUps = followUps;
    this.diaries = diaries;
    this.users = users;
    this.clock = clock;
  }

  private LocalDate today() {
    return ClaimAgeing.today(clock);
  }

  private void status(String user, Long companyId, Long claimId, String status, String remark) {
    users.as(user, () -> statuses.change(companyId, claimId, status, remark));
  }

  private DiaryEntry diary(String user, Long companyId, Long claimId, DiaryInput input) {
    return users.as(user, () -> diaries.add(companyId, claimId, input));
  }

  /**
   * Motor claim (Motor HO): documents complete, the adjuster reviews, the next action is planned; a
   * call is logged and done, a follow-up is due in two days.
   *
   * @param companyId company
   * @param claimId claim
   */
  public void motorInProgress(Long companyId, Long claimId) {
    LocalDate today = today();
    status(OFFICER, companyId, claimId, "NEW_COMPLETE_DOCS", "Police report and photos received");
    status(TL, companyId, claimId, "ADJUSTER_REVIEW", "Insurer appointed the adjuster");
    users.as(
        TL,
        () -> followUps.assignAdjuster(companyId, claimId, "CRAWFORD", "Appointed by the insurer"));
    users.as(
        OFFICER,
        () ->
            followUps.planNextAction(
                companyId, claimId, "Chase the adjuster's inspection report and estimate"));
    DiaryEntry call =
        diary(
            OFFICER,
            companyId,
            claimId,
            new DiaryInput(
                "CALL",
                today,
                null,
                null,
                "Called the assured: vehicle at the dealer for estimate"));
    users.as(
        OFFICER, () -> diaries.markDone(companyId, call.getId(), "Estimate to follow by e-mail"));
    diary(
        OFFICER,
        companyId,
        claimId,
        new DiaryInput(
            "FOLLOW_UP",
            today,
            today.plusDays(FOLLOW_UP_IN_DAYS),
            null,
            "Follow up the adjuster's report"));
  }

  /**
   * Property typhoon claim (Non-Motor HO): with the insurer, then the offer waits for the claimant;
   * the Team Head moves the follow-up on the insurer's advice; a meeting is planned.
   *
   * @param companyId company
   * @param claimId claim
   */
  public void propertyOffer(Long companyId, Long claimId) {
    LocalDate today = today();
    status(TH, companyId, claimId, "INSURER_REVIEW", "Loss advice and photos sent to the insurer");
    status(
        TH,
        companyId,
        claimId,
        "CLAIMANT_OFFER_ACCEPTANCE",
        "Insurer offers PHP 180,000.00 net of deductible");
    users.as(
        TH,
        () ->
            followUps.overrideFollowUp(
                companyId, claimId, today.plusDays(OVERRIDE_IN_DAYS), "INSURER_ADVICE"));
    diary(
        NON_MOTOR,
        companyId,
        claimId,
        new DiaryInput(
            "MEETING",
            today,
            today.plusDays(MEETING_IN_DAYS),
            null,
            "Meet the assured to explain the offer and the deductible"));
  }

  /**
   * Direct-payment motor claim: temporarily closed, the assured has not sent the documents.
   *
   * @param companyId company
   * @param claimId claim
   */
  public void temporarilyClosed(Long companyId, Long claimId) {
    status(
        OFFICER,
        companyId,
        claimId,
        "TEMP_CLOSED_NO_DOCS",
        "Assured has not submitted the documents after two reminders");
    diary(
        OFFICER,
        companyId,
        claimId,
        new DiaryInput("EMAIL", today(), null, null, "Second reminder e-mailed to the assured"));
  }

  /**
   * Insurer-reported theft claim: the insurer issues the LOA and the Team Lead settles and closes.
   *
   * @param companyId company
   * @param claimId claim
   */
  public void settledOnLoa(Long companyId, Long claimId) {
    status(TL, companyId, claimId, "INSURER_LOA_ISSUANCE", "Complete documents with the insurer");
    users.as(
        TL,
        () ->
            closures.settle(
                companyId,
                claimId,
                new Settlement(
                    "SETTLED_LOA_ISSUED",
                    new BigDecimal("850000.00"),
                    today(),
                    "LOA issued by the insurer")));
  }

  /**
   * Property fire claim: closed within the deductible, reopened by the Team Head when further
   * damage is found, and back with the adjuster.
   *
   * @param companyId company
   * @param claimId claim
   */
  public void reopened(Long companyId, Long claimId) {
    status(TH, companyId, claimId, "INSURER_REVIEW", "Estimate sent to the insurer");
    users.as(
        TH,
        () ->
            closures.settle(
                companyId,
                claimId,
                new Settlement(
                    "CLOSED_WITHIN_DEDUCTIBLE", null, null, "Loss within the deductible")));
    users.as(
        TH,
        () ->
            closures.reopen(
                companyId,
                claimId,
                "ADDITIONAL_LOSS",
                "Water damage to the stock found after the first survey"));
    status(TH, companyId, claimId, "ADJUSTER_REVIEW", "Adjuster to inspect the additional loss");
  }

  /**
   * Liability claim just filed without documents (its premium may still be unpaid): the officer
   * plans the document follow-up.
   *
   * @param companyId company
   * @param claimId claim
   */
  public void newlyFiled(Long companyId, Long claimId) {
    LocalDate today = today();
    users.as(
        NON_MOTOR,
        () ->
            followUps.planNextAction(
                companyId, claimId, "Obtain the incident report and the medical certificate"));
    diary(
        NON_MOTOR,
        companyId,
        claimId,
        new DiaryInput(
            "FOLLOW_UP",
            today,
            today.plusDays(FOLLOW_UP_IN_DAYS),
            null,
            "Documents requested from the assured"));
  }

  /**
   * Property claim whose premium is paid but not yet remitted: "With BDOI - For Premium
   * Remittance", the claims special remittance can be requested from the claim.
   *
   * @param companyId company
   * @param claimId claim
   */
  public void awaitingRemittance(Long companyId, Long claimId) {
    status(
        TH,
        companyId,
        claimId,
        "BDOI_PREMIUM_REMITTANCE",
        "Insurer asks for proof of premium remittance before evaluation");
  }
}
