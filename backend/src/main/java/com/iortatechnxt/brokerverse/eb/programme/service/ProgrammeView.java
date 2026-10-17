package com.iortatechnxt.brokerverse.eb.programme.service;

import com.iortatechnxt.brokerverse.eb.domain.EbBorStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdvice;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * A programme page (design 10.1): the programme with its lines, contacts and cycles, each cycle
 * with its renewal advice and Broker on Record status; the current cycle is the latest open one.
 *
 * @param id programme
 * @param programmeNo programme number
 * @param client client id, code and name
 * @param name programme name
 * @param teamCode team
 * @param funding EMPLOYER or VOLUNTARY
 * @param accountOfficer AO user name
 * @param salesUnit sales unit
 * @param renewalEligible flagged for renewal
 * @param status programme status
 * @param createdAt created
 * @param createdBy creator
 * @param lines benefit lines (active and removed)
 * @param contacts HR contacts (active and removed)
 * @param cycles cycles, latest policy year first
 * @param currentCycleId latest open cycle, null when none
 */
public record ProgrammeView(
    Long id,
    String programmeNo,
    EbProgramme.ClientRef client,
    String name,
    String teamCode,
    String funding,
    String accountOfficer,
    String salesUnit,
    boolean renewalEligible,
    String status,
    Instant createdAt,
    String createdBy,
    List<LineView> lines,
    List<ContactView> contacts,
    List<CycleView> cycles,
    Long currentCycleId) {

  /**
   * Builds the view.
   *
   * @param p programme (inside its transaction)
   * @param cycles cycles, latest first
   * @param advices renewal advice per cycle
   * @param bors BOR status per cycle (latest version)
   * @return the view
   */
  public static ProgrammeView of(
      EbProgramme p,
      List<EbCycle> cycles,
      Map<Long, EbRenewalAdvice> advices,
      Map<Long, EbBorStatus> bors) {
    List<CycleView> cycleViews =
        cycles.stream()
            .map(c -> CycleView.of(c, advices.get(c.getId()), bors.get(c.getId())))
            .toList();
    Long current =
        cycles.stream().filter(EbCycle::isOpen).map(EbCycle::getId).findFirst().orElse(null);
    return new ProgrammeView(
        p.getId(),
        p.getProgrammeNo(),
        new EbProgramme.ClientRef(p.getClientId(), p.getClientCode(), p.getClientName()),
        p.getName(),
        p.getTeamCode(),
        p.getFunding().name(),
        p.getAccountOfficer(),
        p.getSalesUnit(),
        p.isRenewalEligible(),
        p.getStatus().name(),
        p.getCreatedAt(),
        p.getCreatedBy(),
        p.getLines().stream().map(LineView::of).toList(),
        p.getContacts().stream().map(ContactView::of).toList(),
        cycleViews,
        current);
  }

  /**
   * A benefit line.
   *
   * @param lineNo line number
   * @param benefitLine HMO, GLI or GPA
   * @param productCode product
   * @param incumbentInsurer insurer party code
   * @param currentPolicyNo current policy
   * @param currentArn current account
   * @param periodFrom period start
   * @param periodTo period end
   * @param headcount members
   * @param active part of the programme
   */
  public record LineView(
      int lineNo,
      String benefitLine,
      String productCode,
      String incumbentInsurer,
      String currentPolicyNo,
      String currentArn,
      LocalDate periodFrom,
      LocalDate periodTo,
      Integer headcount,
      boolean active) {

    static LineView of(EbProgrammeLine l) {
      return new LineView(
          l.getLineNo(),
          l.getBenefitLine(),
          l.getProductCode(),
          l.getIncumbentInsurer(),
          l.getCurrentPolicyNo(),
          l.getCurrentArn(),
          l.getPeriodFrom(),
          l.getPeriodTo(),
          l.getHeadcount(),
          l.isActive());
    }
  }

  /**
   * An HR contact.
   *
   * @param id contact
   * @param name name
   * @param email e-mail
   * @param mobile mobile
   * @param role HR_HEAD, HR_OFFICER or FINANCE
   * @param receivesRa receives the renewal advice
   * @param receivesSoa receives the SOAs
   * @param active active
   */
  public record ContactView(
      Long id,
      String name,
      String email,
      String mobile,
      String role,
      boolean receivesRa,
      boolean receivesSoa,
      boolean active) {

    static ContactView of(EbProgrammeContact c) {
      return new ContactView(
          c.getId(),
          c.getName(),
          c.getEmail(),
          c.getMobile(),
          c.getRole().name(),
          c.isReceivesRa(),
          c.isReceivesSoa(),
          c.isActive());
    }
  }

  /**
   * A cycle.
   *
   * @param id cycle
   * @param cycleNo cycle number
   * @param businessType NEW_BUSINESS or RENEWAL
   * @param policyYear policy year
   * @param targetInception target inception
   * @param stage stage
   * @param remarketing remarketed
   * @param outcome outcome, null while open
   * @param outcomeReason reason of a lost or not renewed cycle
   * @param closedAt closed
   * @param accountArns accounts created at placement
   * @param renewalAdvice the renewal advice, null when none
   * @param borStatus status of the latest BOR version, PENDING when none
   */
  public record CycleView(
      Long id,
      String cycleNo,
      String businessType,
      int policyYear,
      LocalDate targetInception,
      String stage,
      boolean remarketing,
      String outcome,
      String outcomeReason,
      Instant closedAt,
      List<String> accountArns,
      AdviceView renewalAdvice,
      String borStatus) {

    static CycleView of(EbCycle c, EbRenewalAdvice advice, EbBorStatus bor) {
      return new CycleView(
          c.getId(),
          c.getCycleNo(),
          c.getBusinessType().name(),
          c.getPolicyYear(),
          c.getTargetInception(),
          c.getStage().name(),
          c.isRemarketing(),
          c.getOutcome() == null ? null : c.getOutcome().name(),
          c.getOutcomeReason(),
          c.getClosedAt(),
          c.getAccountArns(),
          advice == null ? null : AdviceView.of(advice),
          bor == null ? "PENDING" : bor.name());
    }
  }

  /**
   * The renewal advice of a cycle.
   *
   * @param sentAt sent
   * @param sentBy user or SYSTEM
   * @param manual sent by the AO
   * @param expiryDate expiry announced
   * @param recipients addresses
   * @param remindersSent reminders sent
   * @param lastReminderAt last reminder
   * @param feedbackAt feedback recorded (reminders stopped)
   * @param attachmentId stored advice
   */
  public record AdviceView(
      Instant sentAt,
      String sentBy,
      boolean manual,
      LocalDate expiryDate,
      List<String> recipients,
      int remindersSent,
      Instant lastReminderAt,
      Instant feedbackAt,
      Long attachmentId) {

    static AdviceView of(EbRenewalAdvice a) {
      return new AdviceView(
          a.getSentAt(),
          a.getSentBy(),
          a.isManual(),
          a.getExpiryDate(),
          a.getRecipients(),
          a.getRemindersSent(),
          a.getLastReminderAt(),
          a.getFeedbackAt(),
          a.getAttachmentId());
    }
  }
}
