package com.iortatechnxt.brokerverse.renewal.acceptance.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.QueueSource;
import com.iortatechnxt.brokerverse.booking.service.BookingQueueService;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip;
import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService;
import com.iortatechnxt.brokerverse.renewal.domain.ClosedAs;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalRemark;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalRemarkRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Progression of an accepted renewal (FR-RN-084): the renewal account is fast-tracked to the
 * payment gate, its placement slips are generated when {@code RNW_AUTO_PLACEMENT} is on (the
 * Processing Officer sends them from Placement), it is queued for booking when its policy is
 * issued, and the renewal closes RENEWED when its account (or the account of its New Business path)
 * is booked.
 */
@Service
@Transactional
public class RenewalProgression {

  private final RenewalCandidateRepository candidates;
  private final AccountRepository accounts;
  private final AccountService accountService;
  private final PlacementSlipService slips;
  private final BookingQueueService queue;
  private final RenewalFlow flow;
  private final RenewalParameters parameters;
  private final RenewalNotices notices;
  private final RenewalRemarkRepository remarks;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the progression.
   *
   * @param candidates renewals
   * @param accounts accounts
   * @param accountService account workflow
   * @param slips placement slips
   * @param queue booking queue
   * @param flow workflow
   * @param parameters parameters
   * @param notices notifications
   * @param remarks remarks
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings({"java:S107", "PMD.ExcessiveParameterList"}) // constructor injection
  public RenewalProgression(
      RenewalCandidateRepository candidates,
      AccountRepository accounts,
      AccountService accountService,
      PlacementSlipService slips,
      BookingQueueService queue,
      RenewalFlow flow,
      RenewalParameters parameters,
      RenewalNotices notices,
      RenewalRemarkRepository remarks,
      AuditTrailService audit,
      Clock clock) {
    this.candidates = candidates;
    this.accounts = accounts;
    this.accountService = accountService;
    this.slips = slips;
    this.queue = queue;
    this.flow = flow;
    this.parameters = parameters;
    this.notices = notices;
    this.remarks = remarks;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Fast-tracks the renewal account of an accepted renewal to the payment gate.
   *
   * @param candidateId renewal
   */
  public void fastTrack(Long candidateId) {
    RenewalCandidate c = candidates.findById(candidateId).orElseThrow();
    if (c.getStage() != RenewalStage.ACCEPTED || c.getRenewalArn() == null) {
      return;
    }
    accountService.fastTrackRenewal(c.getRenewalArn());
    flow.system(c, "fast_track", "Renewal account " + c.getRenewalArn() + " fast-tracked");
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        "Renewal account " + c.getRenewalArn() + " fast-tracked to the payment gate");
  }

  /**
   * Records why a step of the progression failed, for Processing.
   *
   * @param candidateId renewal
   * @param text what failed
   */
  public void failed(Long candidateId, String text) {
    candidates
        .findById(candidateId)
        .ifPresent(
            c -> {
              remarks.save(new RenewalRemark(c.getId(), c.getStage(), cut(text)));
              List<String> owners = new ArrayList<>();
              owners.add(c.getAssignedPo());
              notices.users(
                  owners,
                  RenewalCodes.EVENT_ACCEPTED,
                  c,
                  new RenewalNotices.Text(c.getRenewalRef(), text));
            });
  }

  /**
   * Places the renewal account of an accepted renewal when it is ready for placement.
   *
   * @param accountId account
   */
  public void place(Long accountId) {
    Optional<RenewalCandidate> c =
        parameters.autoPlacement()
            ? renewalOf(accountId, RenewalStage.FOR_PLACEMENT_BOOKING)
            : Optional.empty();
    c.ifPresent(candidate -> generateSlips(accountId, candidate));
  }

  private void generateSlips(Long accountId, RenewalCandidate candidate) {
    Optional<RenewalCandidate> c = Optional.of(candidate);
    Account account = accounts.findById(accountId).orElseThrow();
    List<PlacementSlip> generated =
        slips.generate(account.getCompanyId(), List.of(account.getArn()));
    String text =
        generated.size()
            + " placement slip(s) of "
            + account.getArn()
            + " generated; send them from Placement";
    audit.record(RenewalCodes.ENTITY, c.get().getRenewalRef(), AuditAction.CREATE, text);
    List<String> owners = new ArrayList<>();
    owners.add(c.get().getAssignedPo());
    notices.users(
        owners,
        RenewalCodes.EVENT_ACCEPTED,
        c.get(),
        new RenewalNotices.Text(c.get().getRenewalRef() + ": ready for placement", text));
  }

  /**
   * Queues an issued renewal account for booking (in the issuing transaction).
   *
   * @param accountId account
   */
  public void issued(Long accountId) {
    if (renewalOf(accountId, RenewalStage.FOR_PLACEMENT_BOOKING).isEmpty()) {
      return;
    }
    accounts.findById(accountId).ifPresent(a -> queue.enqueueIssued(a, QueueSource.RENEWAL));
  }

  /**
   * Closes the renewal RENEWED when its account, or the account of its New Business path, is
   * booked.
   *
   * @param arn booked account
   * @param invoiceNo booking invoice
   */
  public void booked(String arn, String invoiceNo) {
    Optional<RenewalCandidate> direct =
        candidates
            .findFirstByRenewalArn(arn)
            .filter(c -> c.getStage() == RenewalStage.FOR_PLACEMENT_BOOKING);
    Optional<RenewalCandidate> target = direct.isPresent() ? direct : newBusinessPath(arn);
    target.ifPresent(
        c -> {
          flow.system(c, "renewed", "Booked with invoice " + invoiceNo);
          c.close(ClosedAs.RENEWED, invoiceNo, clock.instant());
          List<String> owners = new ArrayList<>();
          owners.add(c.getAssignedAo());
          owners.add(c.getAssignedPo());
          notices.users(
              owners,
              RenewalCodes.EVENT_RENEWED,
              c,
              new RenewalNotices.Text(
                  c.getRenewalRef() + " renewed", "Booked with invoice " + invoiceNo));
        });
  }

  private Optional<RenewalCandidate> newBusinessPath(String arn) {
    Optional<Account> account = accounts.findByArn(arn);
    String renews =
        account
            .filter(a -> a.getClassification() != null)
            .filter(a -> a.getClassification().businessType() == BusinessType.RENEWAL)
            .map(a -> a.getClassification().renewalOfRef())
            .orElse(null);
    if (renews == null) {
      return Optional.empty();
    }
    Long companyId = account.get().getCompanyId();
    return candidates.findByCompanyIdAndStageIn(companyId, List.of(RenewalStage.NB_PATH)).stream()
        .filter(
            c ->
                Objects.equals(c.getExpiringArn(), renews)
                    || Objects.equals(c.getSourceRef(), renews))
        .findFirst();
  }

  private Optional<RenewalCandidate> renewalOf(Long accountId, RenewalStage stage) {
    return accounts
        .findById(accountId)
        .flatMap(a -> candidates.findFirstByRenewalArn(a.getArn()))
        .filter(c -> c.getStage() == stage);
  }

  private static String cut(String text) {
    return text.length() > RenewalRemark.MAX_LENGTH
        ? text.substring(0, RenewalRemark.MAX_LENGTH)
        : text;
  }
}
