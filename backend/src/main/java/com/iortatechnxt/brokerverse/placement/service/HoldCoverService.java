package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.placement.domain.HoldCover;
import com.iortatechnxt.brokerverse.placement.domain.HoldCover.CoverPeriod;
import com.iortatechnxt.brokerverse.placement.domain.HoldCoverRepository;
import com.iortatechnxt.brokerverse.placement.service.InsurerDirectory.PlacementAddress;
import com.iortatechnxt.brokerverse.placement.service.SlipDocuments.SlipHeader;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hold cover (BRNB.072/103): the 30-day request to the insurer (HOLD_COVER_REQUEST template, PDF by
 * e-mail), the insurer's confirmation with reference and date, a decline, and the expiry monitor
 * that alerts Processing before a hold cover lapses and expires it afterwards. Each change is
 * mirrored on the account through {@code AccountLifecycleService.recordHoldCover}.
 */
@Service
@Transactional
public class HoldCoverService {

  /** Audit entity type. */
  public static final String ENTITY = "HoldCover";

  /** Parameter: days of hold cover. */
  public static final String DAYS = "HOLD_COVER_DAYS";

  /** Parameter: alert lead time in days. */
  public static final String ALERT_DAYS = "HOLD_COVER_ALERT_DAYS";

  private static final int DEFAULT_DAYS = 30;
  private static final int DEFAULT_ALERT_DAYS = 5;
  private static final String TEMPLATE = "HOLD_COVER_REQUEST";
  private static final Set<AccountStatus> IN_PLACEMENT =
      EnumSet.of(
          AccountStatus.READY_FOR_PLACEMENT,
          AccountStatus.PLACED,
          AccountStatus.RETURNED_BY_INSURER);
  private static final List<HoldCoverStatus> OPEN =
      List.of(HoldCoverStatus.REQUESTED, HoldCoverStatus.CONFIRMED);

  private final HoldCoverRepository holdCovers;
  private final PlacementAccounts accounts;
  private final InsurerDirectory insurers;
  private final SlipDocuments documents;
  private final DocTemplateService templates;
  private final MessageService messages;
  private final NotificationService notifications;
  private final AccountLifecycleService lifecycle;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param holdCovers hold covers
   * @param accounts account look-ups
   * @param insurers insurer addressing
   * @param documents document rendering
   * @param templates document templates
   * @param messages outbound e-mail
   * @param notifications in-app notifications
   * @param lifecycle account lifecycle
   * @param parameters business parameters
   * @param audit audit trail
   * @param clock clock
   */
  public HoldCoverService(
      HoldCoverRepository holdCovers,
      PlacementAccounts accounts,
      InsurerDirectory insurers,
      SlipDocuments documents,
      DocTemplateService templates,
      MessageService messages,
      NotificationService notifications,
      AccountLifecycleService lifecycle,
      SystemParameterService parameters,
      AuditTrailService audit,
      Clock clock) {
    this.holdCovers = holdCovers;
    this.accounts = accounts;
    this.insurers = insurers;
    this.documents = documents;
    this.templates = templates;
    this.messages = messages;
    this.notifications = notifications;
    this.lifecycle = lifecycle;
    this.parameters = parameters;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Sends the hold cover request to the insurer (BRNB.072).
   *
   * @param arn account in placement
   * @param request start date and optional e-mail overrides
   * @return the hold cover
   */
  public HoldCover request(String arn, HoldCoverRequest request) {
    Account account = accounts.require(arn);
    if (!IN_PLACEMENT.contains(account.getStatus())) {
      throw new BusinessRuleException(
          "HOLD_COVER_NOT_ALLOWED",
          "A hold cover is requested while the account is being placed, not "
              + account.getStatus());
    }
    if (current(arn).filter(HoldCover::isOpen).isPresent()) {
      throw new BusinessRuleException(
          "HOLD_COVER_OPEN", "Account " + arn + " already has a hold cover requested or confirmed");
    }
    PlacementAddress address =
        insurers.address(
            account.getCompanyId(), account.getInsurerCode(), account.getInsurerBranch());
    LocalDate today = LocalDate.now(clock);
    LocalDate start = request.startDate() == null ? today : request.startDate();
    LocalDate expiry = start.plusDays(parameters.intValue(DAYS, DEFAULT_DAYS));
    MergedText text =
        templates.merge(TEMPLATE, today, Map.of("startDate", start, "reference", arn));
    byte[] pdf =
        documents.holdCoverPdf(
            new SlipHeader(account.getCompanyId(), arn, account.getInsurerCode(), address, text),
            account,
            start,
            expiry);
    messages.queueEmail(
        new OutboundEmail(
            account.getCompanyId(),
            "HOLD_COVER",
            request.to().isEmpty() ? address.recipients() : request.to(),
            List.of(),
            "Hold cover request - " + arn + " - " + account.getClientName(),
            text.text(),
            List.of(new MessageFile("HOLD_COVER_" + arn + ".pdf", "application/pdf", pdf)),
            null,
            new RecordLink(AccountService.ENTITY, String.valueOf(account.getId()), arn)));
    HoldCover saved =
        holdCovers.save(
            new HoldCover(
                account.getCompanyId(),
                account.getId(),
                arn,
                account.getInsurerCode(),
                new CoverPeriod(start, expiry)));
    lifecycle.recordHoldCover(arn, HoldCoverStatus.REQUESTED, null, start);
    audit.record(
        ENTITY, arn, AuditAction.CREATE, "Hold cover requested " + start + " to " + expiry);
    return saved;
  }

  /**
   * Records the insurer's confirmation (BRNB.103); a confirmation received without a request is
   * recorded as a new hold cover.
   *
   * @param arn account
   * @param confirmation insurer reference, date and optional expiry
   * @return the hold cover
   */
  public HoldCover confirm(String arn, HoldCoverConfirmation confirmation) {
    Account account = accounts.require(arn);
    if (confirmation.reference() == null || confirmation.reference().isBlank()) {
      throw new BusinessRuleException("HOLD_COVER_REFERENCE", "Enter the insurer's reference");
    }
    LocalDate date =
        confirmation.confirmedOn() == null ? LocalDate.now(clock) : confirmation.confirmedOn();
    HoldCover cover =
        current(arn)
            .filter(HoldCover::isOpen)
            .orElseGet(
                () ->
                    holdCovers.save(
                        new HoldCover(
                            account.getCompanyId(),
                            account.getId(),
                            arn,
                            requireInsurer(account),
                            new CoverPeriod(
                                date, date.plusDays(parameters.intValue(DAYS, DEFAULT_DAYS))))));
    String insurer =
        confirmation.insurerCode() == null || confirmation.insurerCode().isBlank()
            ? cover.getInsurerCode()
            : confirmation.insurerCode().strip();
    cover.confirm(insurer, confirmation.reference().strip(), date, confirmation.expiryDate());
    lifecycle.recordHoldCover(arn, HoldCoverStatus.CONFIRMED, cover.getInsurerRef(), date);
    audit.record(
        ENTITY,
        arn,
        AuditAction.UPDATE,
        "Hold cover confirmed by "
            + insurer
            + " ref "
            + cover.getInsurerRef()
            + " until "
            + cover.getExpiryDate());
    return cover;
  }

  private static String requireInsurer(Account account) {
    if (account.getInsurerCode() == null) {
      throw new BusinessRuleException("INSURER_NOT_SET", "Choose the insurer on the account first");
    }
    return account.getInsurerCode();
  }

  /**
   * Records that the insurer declined the hold cover.
   *
   * @param arn account
   * @param reference insurer reference, may be null
   * @return the hold cover
   */
  public HoldCover decline(String arn, String reference) {
    HoldCover cover =
        current(arn)
            .filter(HoldCover::isOpen)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "HOLD_COVER_NONE", "Account " + arn + " has no open hold cover"));
    cover.decline(reference);
    lifecycle.recordHoldCover(arn, HoldCoverStatus.DECLINED, reference, LocalDate.now(clock));
    audit.record(ENTITY, arn, AuditAction.UPDATE, "Hold cover declined by the insurer");
    return cover;
  }

  /**
   * Expiry monitor (job HOLD_COVER_EXPIRY): alerts the holders of PLACEMENT_MANAGE once for each
   * open hold cover expiring within the alert lead time, and expires those past their expiry date
   * while the policy is still awaited.
   *
   * @param today business date
   * @return alerts and expiries
   */
  public ExpiryRun runExpiry(LocalDate today) {
    LocalDate horizon = today.plusDays(parameters.intValue(ALERT_DAYS, DEFAULT_ALERT_DAYS));
    int alerted = 0;
    int expired = 0;
    for (HoldCover cover :
        holdCovers.findByStatusInAndExpiryDateLessThanEqualOrderByExpiryDateAsc(OPEN, horizon)) {
      Account account = accounts.require(cover.getArn());
      boolean awaitingPolicy = IN_PLACEMENT.contains(account.getStatus());
      if (cover.getExpiryDate().isBefore(today)) {
        cover.expire();
        if (awaitingPolicy) {
          lifecycle.recordHoldCover(
              cover.getArn(), HoldCoverStatus.EXPIRED, cover.getInsurerRef(), today);
          notify(cover, "Hold cover expired: ", "expired on ");
        }
        expired++;
      } else if (awaitingPolicy && cover.getAlertedOn() == null) {
        notify(cover, "Hold cover expiring: ", "expires on ");
        cover.markAlerted(today);
        alerted++;
      }
    }
    return new ExpiryRun(alerted, expired);
  }

  private void notify(HoldCover cover, String title, String verb) {
    notifications.notifyPermission(
        "PLACEMENT_MANAGE",
        new Notice(
            title + cover.getArn(),
            "The hold cover of "
                + cover.getInsurerCode()
                + " "
                + verb
                + cover.getExpiryDate()
                + "; the policy has not been received yet.",
            "/placement/accounts/" + cover.getArn(),
            AccountService.ENTITY,
            String.valueOf(cover.getAccountId())));
  }

  /**
   * The latest hold cover of an account.
   *
   * @param arn Account Reference Number
   * @return hold cover
   */
  @Transactional(readOnly = true)
  public Optional<HoldCover> current(String arn) {
    return holdCovers.findFirstByArnOrderByIdDesc(arn);
  }

  /**
   * A hold cover request.
   *
   * @param startDate first day of the hold cover; today when null
   * @param to recipients; the insurer branch mailbox when empty
   */
  public record HoldCoverRequest(LocalDate startDate, List<String> to) {

    /** Defensive copy. */
    public HoldCoverRequest {
      to = to == null ? List.of() : List.copyOf(to);
    }
  }

  /**
   * The insurer's confirmation.
   *
   * @param insurerCode insurer that confirmed; the requested insurer when blank
   * @param reference insurer reference
   * @param confirmedOn confirmation date; today when null
   * @param expiryDate expiry confirmed by the insurer; the requested expiry when null
   */
  public record HoldCoverConfirmation(
      String insurerCode, String reference, LocalDate confirmedOn, LocalDate expiryDate) {}

  /**
   * Outcome of an expiry run.
   *
   * @param alerted hold covers alerted
   * @param expired hold covers expired
   */
  public record ExpiryRun(int alerted, int expired) {}
}
