package com.iortatechnxt.brokerverse.eb.member.service;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbMember;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbResponsibleParty;
import com.iortatechnxt.brokerverse.eb.domain.EbRosterVersion;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * What a member change does once validated (BRID-013, 025; FR-EB-055, 057): a change with a
 * premium effect raises an endorsement request in Operations on the booked invoice of the line's
 * current account (source reference {@code EBM-...}), and closing the change applies its lines to
 * the accepted roster, opening an HMO card item for each member added on an HMO line.
 */
@Component
public class MemberChangeEffects {

  /** Tracked item type of an HMO card. */
  static final String HMO_CARD = "HMO_CARD";

  /** Tracked item type of a billing. */
  static final String BILLING = "BILLING_INVOICE";

  private static final String HMO = "HMO";
  private static final String ENDORSEMENT_TYPE = "FIN_ITEMS";
  private static final String REQUEST_TYPE = "PREMIUM_RATE_CHANGE";

  private final EndorsementRequestService endorsements;
  private final OpsInvoiceRepository invoices;
  private final EbMemberRepository members;
  private final TrackedItemService trackedItems;
  private final EbParameters parameters;
  private final EbWorkingDays workingDays;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param endorsements endorsement requests of Operations
   * @param invoices invoice ledger
   * @param members roster members
   * @param trackedItems HMO card items
   * @param parameters card TAT
   * @param workingDays working-day calendar
   * @param clock clock
   */
  public MemberChangeEffects(
      EndorsementRequestService endorsements,
      OpsInvoiceRepository invoices,
      EbMemberRepository members,
      TrackedItemService trackedItems,
      EbParameters parameters,
      EbWorkingDays workingDays,
      Clock clock) {
    this.endorsements = endorsements;
    this.invoices = invoices;
    this.members = members;
    this.trackedItems = trackedItems;
    this.parameters = parameters;
    this.workingDays = workingDays;
    this.clock = clock;
  }

  /**
   * Raises the endorsement request of a financial change on the booked invoice of the line.
   *
   * @param change validated financial change
   * @param line programme line
   */
  void raiseEndorsement(EbMemberChange change, EbProgrammeLine line) {
    OpsInvoice invoice =
        line.getCurrentArn() == null
            ? null
            : invoices.findByArnOrderByPolicyYearAscIdAsc(line.getCurrentArn()).stream()
                .filter(i -> i.getKind() == InvoiceKind.BOOKING && !i.isCancelled())
                .max(Comparator.comparing(OpsInvoice::getId))
                .orElse(null);
    if (invoice == null) {
      throw new BusinessRuleException(
          "EB_NO_INVOICE",
          "Line " + line.getLineNo() + " has no booked invoice for the endorsement request");
    }
    LocalDate effective =
        change.getLines().stream()
            .map(EbMemberChange.Line::getEffectiveDate)
            .min(Comparator.naturalOrder())
            .orElse(BusinessClock.today(clock));
    EndorsementRequest request =
        endorsements.create(
            new RequestDraft(
                invoice.getInvoiceNo(),
                new RequestTerms(
                    ENDORSEMENT_TYPE,
                    REQUEST_TYPE,
                    null,
                    change.getChangeNo(),
                    effective,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Member change " + change.getChangeNo() + ": " + summary(change),
                    change.getBillingRef() == null ? null : "Insurer billing " + change.getBillingRef()),
                new AmountInput(change.getBilledAmount(), null, null, null, null, null, null, null),
                null,
                null));
    change.endorsementRaised(request.getId(), request.getRequestNo());
  }

  /**
   * A short description of the lines, e.g. "2 add, 1 delete".
   *
   * @param change change
   * @return text
   */
  static String summary(EbMemberChange change) {
    Map<EbMemberChange.Action, Long> counts =
        change.getLines().stream()
            .collect(Collectors.groupingBy(EbMemberChange.Line::getAction, Collectors.counting()));
    return counts.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> e.getValue() + " " + e.getKey().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '))
        .collect(Collectors.joining(", "));
  }

  /**
   * Applies the lines of a change to the accepted roster.
   *
   * @param programme programme
   * @param change change
   * @param roster accepted roster of the change's policy year
   */
  void apply(EbProgramme programme, EbMemberChange change, EbRosterVersion roster) {
    Map<Long, EbMember> byId =
        members.findByRosterVersionIdOrderByEmployeeNoAsc(roster.getId()).stream()
            .collect(Collectors.toMap(EbMember::getId, Function.identity()));
    for (EbMemberChange.Line line : change.getLines()) {
      switch (line.getAction()) {
        case ADD -> add(programme, change, roster, line);
        case DELETE -> byId.get(line.getMemberId()).delete(line.getEffectiveDate().minusDays(1));
        case CHANGE_PLAN -> byId.get(line.getMemberId()).changePlan(line.getPlanCode());
        case CHANGE_DATA -> byId.get(line.getMemberId()).update(line.memberData());
        default -> throw new IllegalStateException("Unknown action " + line.getAction());
      }
    }
    members.flush();
    roster.recount((int) members.countByRosterVersionIdAndStatus(roster.getId(), EbMember.Status.ACTIVE));
  }

  private void add(
      EbProgramme programme, EbMemberChange change, EbRosterVersion roster, EbMemberChange.Line line) {
    EbMember member =
        members.save(new EbMember(roster, line.getEmployeeNo(), line.memberData(), line.getEffectiveDate()));
    line.member(member.getId());
    if (!HMO.equals(change.getBenefitLine())) {
      return;
    }
    EbProgrammeLine programmeLine = programme.line(change.getLineNo());
    LocalDate due =
        workingDays.plus(
            programme.getCompanyId(), BusinessClock.today(clock), parameters.tatDays(TatActivity.CARDS));
    trackedItems.openLinked(
        programme,
        HMO_CARD,
        new EbTrackedItem.Details(
            "HMO card of " + member.displayName(),
            member.getEmployeeNo(),
            change.getChangeNo(),
            programmeLine.getCurrentArn(),
            EbResponsibleParty.INSURER,
            programmeLine.getIncumbentInsurer(),
            null,
            due,
            null),
        member.getId(),
        change.getId());
  }
}
