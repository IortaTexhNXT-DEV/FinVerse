package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.csf.domain.CheckResult;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivity;
import com.iortatechnxt.brokerverse.csf.domain.CsfVerification;
import com.iortatechnxt.brokerverse.csf.domain.CsfVerificationRepository;
import com.iortatechnxt.brokerverse.csf.domain.VerificationResult;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.CheckView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.VerificationView;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verification of the caller before a contact change (FR-CSF-020; e-mail topic 5): the checklist of
 * list CSF_VERIFY_CHECK with the outcome of each check; PASSED when at least {@code
 * CSF_VERIFY_MIN_MATCHES} checks match, valid {@code CSF_VERIFICATION_VALID_MINUTES}. Repeated
 * failed verifications of one client in a day ({@code CSF_VERIFY_MAX_FAILS}) raise the alert {@code
 * CSF_VERIFICATION_FAILED_REPEAT}.
 */
@Service
@Transactional
public class VerificationService {

  private final CsfVerificationRepository verifications;
  private final CsfClients clients;
  private final LovService lovs;
  private final CsfParameters parameters;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final ActivityLog activity;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param verifications verifications
   * @param clients clients of the company
   * @param lovs lists of values
   * @param parameters CSF parameters
   * @param alerts alerts
   * @param audit audit trail
   * @param activity activity log
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators
  public VerificationService(
      CsfVerificationRepository verifications,
      CsfClients clients,
      LovService lovs,
      CsfParameters parameters,
      AlertService alerts,
      AuditTrailService audit,
      ActivityLog activity,
      CurrentUser currentUser,
      Clock clock) {
    this.verifications = verifications;
    this.clients = clients;
    this.lovs = lovs;
    this.parameters = parameters;
    this.alerts = alerts;
    this.audit = audit;
    this.activity = activity;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records the verification of a caller.
   *
   * @param companyId company
   * @param clientId client
   * @param request channel, checks and remarks
   * @return the verification
   */
  public VerificationView verify(Long companyId, Long clientId, VerifyRequest request) {
    Client client = clients.require(companyId, clientId);
    LocalDate today = BusinessClock.today(clock);
    if (request.channel() == null || request.channel().isBlank()) {
      throw new BusinessRuleException("CSF_CHANNEL_REQUIRED", "Select the channel of the contact");
    }
    lovs.requireValid(CsfCodes.LOV_CHANNEL, request.channel(), today);
    requireChecks(request.checks(), today);
    Instant now = clock.instant();
    CsfVerification saved =
        verifications.save(
            new CsfVerification(
                CsfClients.refOf(client),
                request.checks().stream().map(c -> new CheckResult(c.code(), c.matched())).toList(),
                new CsfVerification.Outcome(
                    request.channel(),
                    parameters.verifyMinMatches(),
                    now,
                    now.plus(Duration.ofMinutes(parameters.verificationValidMinutes()))),
                currentUser.username(),
                blankToNull(request.remarks())));
    audit.record(
        CsfCodes.ENTITY_VERIFICATION,
        saved.getId(),
        AuditAction.CREATE,
        "Caller of "
            + client.getCode()
            + " "
            + saved.getResult().name().toLowerCase(Locale.ROOT)
            + " the verification: "
            + saved.getMatches()
            + " of "
            + saved.getChecks().size()
            + " checks matched");
    activity.record(
        companyId,
        ActivityAction.VERIFY,
        new CsfActivity.Subject(
            client.getId(),
            client.getCode(),
            String.valueOf(saved.getId()),
            saved.getResult().name() + " " + saved.getMatches() + "/" + saved.getChecks().size()));
    if (saved.getResult() == VerificationResult.FAILED) {
      alertOnRepeatedFailures(client, today);
    }
    return view(saved);
  }

  private void requireChecks(List<CheckAnswer> checks, LocalDate today) {
    if (checks == null || checks.isEmpty()) {
      throw new BusinessRuleException(
          "CSF_VERIFICATION_CHECKS_REQUIRED", "Record the result of the verification checks");
    }
    Set<String> seen = new HashSet<>();
    for (CheckAnswer c : checks) {
      lovs.requireValid(CsfCodes.LOV_VERIFY_CHECK, c.code(), today);
      if (!seen.add(c.code())) {
        throw new BusinessRuleException(
            "CSF_VERIFICATION_CHECK_TWICE",
            "Record each verification check once: "
                + lovs.label(CsfCodes.LOV_VERIFY_CHECK, c.code()));
      }
    }
  }

  private void alertOnRepeatedFailures(Client client, LocalDate today) {
    long failed =
        verifications.countByClientIdAndResultAndVerifiedAtGreaterThanEqual(
            client.getId(), VerificationResult.FAILED, BusinessClock.startOf(today));
    int max = parameters.verifyMaxFails();
    if (failed >= max) {
      alerts.raise(
          CsfCodes.ALERT_VERIFICATION_FAILED,
          new AlertFacts(
              client.getCompanyId(),
              null,
              CsfCodes.ENTITY_CLIENT,
              String.valueOf(client.getId()),
              failed
                  + " failed caller verifications of "
                  + client.getCode()
                  + " "
                  + client.getDisplayName()
                  + " today",
              null,
              CsfCodes.ALERT_VERIFICATION_FAILED + ":" + client.getId() + ":" + today));
    }
  }

  /**
   * The verification of a client that still allows a change.
   *
   * @param clientId client
   * @return passed verification within its validity, empty when none
   */
  @Transactional(readOnly = true)
  public Optional<VerificationView> current(Long clientId) {
    Instant now = clock.instant();
    return verifications
        .findFirstByClientIdOrderByVerifiedAtDescIdDesc(clientId)
        .filter(v -> v.allowsChangeAt(now))
        .map(VerificationService::view);
  }

  /**
   * The verification a change relies on: of the same client, passed and still valid.
   *
   * @param clientId client
   * @param verificationId verification
   * @return the verification
   */
  @Transactional(readOnly = true)
  public CsfVerification requireValid(Long clientId, Long verificationId) {
    CsfVerification v =
        verificationId == null ? null : verifications.findById(verificationId).orElse(null);
    if (v == null || !Objects.equals(v.getClientId(), clientId)) {
      throw new BusinessRuleException(
          "CSF_VERIFICATION_REQUIRED", "Verify the caller before changing the contact details");
    }
    if (v.getResult() != VerificationResult.PASSED) {
      throw new BusinessRuleException(
          "CSF_VERIFICATION_FAILED",
          "The caller could not be verified. The contact details cannot be changed");
    }
    if (!v.allowsChangeAt(clock.instant())) {
      throw new BusinessRuleException(
          "CSF_VERIFICATION_EXPIRED",
          "The verification of the caller has expired. Verify the caller again");
    }
    return v;
  }

  /**
   * A verification as shown.
   *
   * @param v verification
   * @return view
   */
  static VerificationView view(CsfVerification v) {
    return new VerificationView(
        v.getId(),
        v.getChannel(),
        v.getResult().name(),
        v.getMatches(),
        v.getRequired(),
        v.getChecks().stream().map(c -> new CheckView(c.getCheckCode(), c.isMatched())).toList(),
        v.getVerifiedAt(),
        v.getValidUntil(),
        v.getAgent());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A verification to record.
   *
   * @param channel channel (list CSF_CHANNEL)
   * @param checks checks with their outcome
   * @param remarks remarks, may be null
   */
  public record VerifyRequest(String channel, List<CheckAnswer> checks, String remarks) {

    /** Defensive copy. */
    public VerifyRequest {
      checks = checks == null ? List.of() : List.copyOf(checks);
    }
  }

  /**
   * One check and whether the caller's answer matched.
   *
   * @param code check (list CSF_VERIFY_CHECK)
   * @param matched matched
   */
  public record CheckAnswer(String code, boolean matched) {}
}
