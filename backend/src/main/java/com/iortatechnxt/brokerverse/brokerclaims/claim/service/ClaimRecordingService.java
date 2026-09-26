package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimSource;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService.NewLine;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService.LocationPick;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a claim on a cover (BRCLM.003/004/006/009/016/037/039/041/043; FR-CL-011): cover (ARN,
 * policy year and version at the loss date) and loss data are mandatory; the snapshot copies the
 * policy number, period, sum insured, lead insurer, Marketing unit, AO and branch; the currency is
 * the cover's, else {@code BCL_DEFAULT_CURRENCY}; the claimant is the assured; the premium is
 * checked at once (BRCLM.001, an unpaid cover is saved and flagged); the locations and insurer
 * lines are linked; the workflow case opens in stage NEW in the handler's queue. The first status
 * and the next follow-up date are set by the status engine (wave CL1-B) on {@link ClaimRecorded}.
 */
@Service
@Transactional
public class ClaimRecordingService {

  /** Error code of a loss date outside the cover period; the screen asks for confirmation. */
  public static final String OUTSIDE_COVER = "BCL_LOSS_OUTSIDE_COVER";

  /** First status when none is chosen (phase NEW). */
  public static final String DEFAULT_STATUS = "NEW_INCOMPLETE_DOCS";

  private static final List<String> FIRST_STATUSES = List.of("NEW_COMPLETE_DOCS", DEFAULT_STATUS);
  private static final DateTimeFormatter DAY =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

  private final BrokerClaimRepository claims;
  private final CoverService covers;
  private final PremiumCheckService premiums;
  private final ClaimLocationService locations;
  private final InsurerClaimService insurers;
  private final HandlerDirectory handlers;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final WorkflowService workflow;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final ApplicationEventPublisher events;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param claims claims
   * @param covers covers
   * @param premiums premium check
   * @param locations claim locations
   * @param insurers insurer lines
   * @param handlers handler units
   * @param numbers claim numbers
   * @param lovs lists of values
   * @param workflow workflow BCL_CLAIM
   * @param notifications AO notification
   * @param audit audit trail
   * @param events ClaimRecorded
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ClaimRecordingService(
      BrokerClaimRepository claims,
      CoverService covers,
      PremiumCheckService premiums,
      ClaimLocationService locations,
      InsurerClaimService insurers,
      HandlerDirectory handlers,
      DocumentNumberService numbers,
      LovService lovs,
      WorkflowService workflow,
      NotificationService notifications,
      AuditTrailService audit,
      ApplicationEventPublisher events,
      CurrentUser currentUser,
      Clock clock) {
    this.claims = claims;
    this.covers = covers;
    this.premiums = premiums;
    this.locations = locations;
    this.insurers = insurers;
    this.handlers = handlers;
    this.numbers = numbers;
    this.lovs = lovs;
    this.workflow = workflow;
    this.notifications = notifications;
    this.audit = audit;
    this.events = events;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a claim.
   *
   * @param companyId company
   * @param request cover, loss, locations and insurers
   * @return the claim
   */
  public Claim record(Long companyId, NewClaim request) {
    LocalDate today = LocalDate.now(clock);
    validate(request, today);
    Account account = covers.account(companyId, request.arn());
    CoverSnapshot cover = covers.snapshot(account, request.policyYear(), request.loss().lossDate());
    LossDetails loss =
        LossDetails.of(request.loss(), request.amounts(), account.getClientName(), today);
    if (!cover.covers(loss.getLossDate()) && !request.confirmOutsidePeriod()) {
      throw new BusinessRuleException(
          OUTSIDE_COVER,
          "The loss date is outside the cover period "
              + day(cover.getPeriodFrom())
              + " to "
              + day(cover.getPeriodTo())
              + ". Confirm to continue");
    }
    String username = currentUser.username();
    HandlerDirectory.Handler handler = handlers.of(username);
    Claim claim =
        claims.save(
            new Claim(
                companyId,
                numbers.next(ClaimCodes.CLAIM_NO_PREFIX + "-" + today.getYear()),
                new Claim.Origin(
                    request.source(), username, handler.unitCode(), handler.branchId(), null),
                cover,
                loss));
    locations.link(claim, request.locations());
    addInsurers(claim, account, request);
    premiums.check(claim);
    open(claim);
    events.publishEvent(
        new ClaimRecorded(
            claim.getId(),
            companyId,
            claim.getClaimNo(),
            request.initialStatus() == null ? DEFAULT_STATUS : request.initialStatus(),
            username,
            clock.instant()));
    return claim;
  }

  private void validate(NewClaim request, LocalDate today) {
    LossDetails.Loss loss = request.loss();
    require(!blank(request.arn()), "BCL_COVER_REQUIRED", "Select the cover of the claim");
    require(!blank(loss.lossNature()), "BCL_LOSS_NATURE_REQUIRED", "Select the nature of loss");
    require(!blank(loss.claimType()), "BCL_CLAIM_TYPE_REQUIRED", "Select the claim type");
    require(
        !blank(loss.lossDescription()), "BCL_DESCRIPTION_REQUIRED", "Enter the loss description");
    validateCodes(loss, today);
    validateSource(request);
  }

  private static void require(boolean valid, String code, String message) {
    if (!valid) {
      throw new BusinessRuleException(code, message);
    }
  }

  private void validateCodes(LossDetails.Loss loss, LocalDate today) {
    lovs.requireValid(ClaimCodes.LOV_LOSS_NATURE, loss.lossNature(), today);
    lovs.requireValid(ClaimCodes.LOV_CLAIM_TYPE, loss.claimType(), today);
    lovs.validateOptional(
        ClaimCodes.LOV_CATASTROPHE,
        blank(loss.catastropheCode()) ? null : loss.catastropheCode(),
        today);
  }

  private static void validateSource(NewClaim request) {
    if (request.initialStatus() != null && !FIRST_STATUSES.contains(request.initialStatus())) {
      throw new BusinessRuleException(
          "BCL_FIRST_STATUS", "The first status must be a newly filed claim status");
    }
    boolean numbered = request.insurers().stream().anyMatch(l -> !blank(l.insurerClaimNo()));
    if (request.source() == ClaimSource.INSURER_REPORTED && !numbered) {
      throw new BusinessRuleException(
          "BCL_INSURER_CLAIM_NO_REQUIRED",
          "Enter the insurer's claim number of an insurer-reported claim");
    }
    if (request.source() == ClaimSource.MIGRATED) {
      throw new BusinessRuleException(
          "BCL_SOURCE_NOT_ALLOWED", "Migrated claims come from the legacy migration only (CLQ14)");
    }
  }

  private void addInsurers(Claim claim, Account account, NewClaim request) {
    List<NewLine> lines = request.insurers();
    if (lines.isEmpty()) {
      List<OpsInvoiceShare> shares = covers.shares(account, request.policyYear());
      lines =
          shares.stream()
              .map(
                  s ->
                      new NewLine(
                          s.insurerCode(),
                          s.sharePct(),
                          null,
                          null,
                          shares.size() == 1 ? request.amounts().initialReserve() : null))
              .toList();
    }
    lines.forEach(l -> insurers.add(claim, l, request.confirmReuse()));
  }

  private void open(Claim claim) {
    CoverSnapshot cover = claim.getCover();
    workflow.start(
        new StartCase(
            claim.getCompanyId(),
            ClaimCodes.WORKFLOW,
            new CaseRecord(
                ClaimCodes.ENTITY_TYPE,
                String.valueOf(claim.getId()),
                claim.getClaimNo(),
                cover.getAssuredName() + " - " + cover.getProductCode() + " " + cover.getArn(),
                "/claims-handling/" + claim.getId(),
                claim.getUnitCode()),
            null));
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.CREATE,
        "Recorded on "
            + cover.getArn()
            + " year "
            + cover.getPolicyYear()
            + " ("
            + (cover.getPolicyNo() == null ? "policy number pending" : cover.getPolicyNo())
            + ", "
            + cover.versionLabel()
            + "), loss "
            + claim.getLoss().getLossDate()
            + ", reported "
            + claim.getLoss().getReportedDate()
            + ", source "
            + claim.getSource()
            + ", premium "
            + cover.getPremiumStatus());
    if (cover.getAccountOfficer() != null) {
      notifications.notifyUser(
          cover.getAccountOfficer(),
          new Notice(
              "Claim " + claim.getClaimNo() + " recorded",
              cover.getAssuredName()
                  + " - "
                  + cover.getArn()
                  + ", loss of "
                  + claim.getLoss().getLossDate(),
              "/claims-handling/" + claim.getId(),
              ClaimCodes.ENTITY_TYPE,
              String.valueOf(claim.getId())),
          "BCL_CLAIM_ASSIGNED");
    }
  }

  private static String day(LocalDate date) {
    return date == null ? "-" : DAY.format(date);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * A claim to record.
   *
   * @param arn cover
   * @param policyYear policy year of the cover
   * @param source BDOI notice or insurer-reported
   * @param initialStatus first status (phase NEW), {@value #DEFAULT_STATUS} when null
   * @param loss loss data
   * @param amounts claim amount, deductible, initial loss reserve
   * @param locations locations of the cover, may be empty
   * @param insurers insurer lines; the invoice shares when empty
   * @param confirmOutsidePeriod the user confirmed a loss date outside the cover period
   * @param confirmReuse the user confirmed an insurer claim number found on another claim
   */
  public record NewClaim(
      String arn,
      int policyYear,
      ClaimSource source,
      String initialStatus,
      LossDetails.Loss loss,
      LossDetails.Amounts amounts,
      List<LocationPick> locations,
      List<NewLine> insurers,
      boolean confirmOutsidePeriod,
      boolean confirmReuse) {

    /** Defensive copies. */
    public NewClaim {
      source = source == null ? ClaimSource.BDOI_NOTICE : source;
      locations = locations == null ? List.of() : List.copyOf(locations);
      insurers = insurers == null ? List.of() : List.copyOf(insurers);
    }
  }
}
