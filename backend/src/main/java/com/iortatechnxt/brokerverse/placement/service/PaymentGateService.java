package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.placement.domain.EvidenceKind;
import com.iortatechnxt.brokerverse.placement.domain.GateRule;
import com.iortatechnxt.brokerverse.placement.domain.PaymentEvidence;
import com.iortatechnxt.brokerverse.placement.domain.PaymentEvidence.EvidenceAccount;
import com.iortatechnxt.brokerverse.placement.domain.PaymentEvidence.EvidenceDetail;
import com.iortatechnxt.brokerverse.placement.domain.PaymentEvidence.EvidenceOrigin;
import com.iortatechnxt.brokerverse.placement.domain.PaymentEvidenceRepository;
import com.iortatechnxt.brokerverse.placement.domain.PaymentGateRule;
import com.iortatechnxt.brokerverse.placement.domain.PaymentGateRuleRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The NB_ACCOUNT payment gate {@code payment_confirmed} (BRD 2.3.1, BRNB.068/114). The rule of an
 * account comes from the rule table by market segment and product line: CBG Fire and Motor need a
 * matched payment, Other Lines a recorded client confirmation; direct-payment accounts skip the
 * gate. Every decision keeps its evidence; opening the gate calls {@code
 * AccountLifecycleService.markPaymentConfirmed}.
 */
@Service
@Transactional
public class PaymentGateService {

  /** Audit entity type. */
  public static final String ENTITY = "PaymentGate";

  /** Evidence source of user confirmations. */
  public static final String MANUAL = "MANUAL";

  private static final String CHANNEL_LOV = "CLIENT_CONFIRMATION_CHANNEL";
  private static final int MAX_SOURCE = 60;

  private final PaymentGateRuleRepository rules;
  private final PaymentEvidenceRepository evidence;
  private final PlacementAccounts accounts;
  private final AccountLifecycleService lifecycle;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules gate rules
   * @param evidence gate evidence
   * @param accounts account look-ups
   * @param lifecycle account lifecycle
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PaymentGateService(
      PaymentGateRuleRepository rules,
      PaymentEvidenceRepository evidence,
      PlacementAccounts accounts,
      AccountLifecycleService lifecycle,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.rules = rules;
    this.evidence = evidence;
    this.accounts = accounts;
    this.lifecycle = lifecycle;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The active rules, highest priority first.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<PaymentGateRule> rules() {
    return rules.findByActiveTrueOrderByPriorityAsc();
  }

  /**
   * The gate of an account: rule, whether it is open and the evidence recorded.
   *
   * @param arn Account Reference Number
   * @return gate view
   */
  @Transactional(readOnly = true)
  public GateView view(String arn) {
    Account account = accounts.require(arn);
    RuleChoice rule = ruleFor(account);
    boolean open =
        account.isDirectPayment()
            || account.getLifecycle().getPaymentStatus() != PaymentStatus.UNPAID;
    return new GateView(
        account, rule.rule(), rule.description(), open, evidence.findByArnOrderByIdAsc(arn));
  }

  /**
   * The rule that applies to an account.
   *
   * @param account account
   * @return rule and its description
   */
  @Transactional(readOnly = true)
  public RuleChoice ruleFor(Account account) {
    if (account.isDirectPayment()) {
      return new RuleChoice(
          GateRule.DIRECT_PAYMENT, "Paid directly to the insurer: the payment gate does not apply");
    }
    return rules.findByActiveTrueOrderByPriorityAsc().stream()
        .filter(r -> r.matches(account.getMarketSegment(), account.getLineCode()))
        .findFirst()
        .map(r -> new RuleChoice(r.getRule(), r.getDescription()))
        .orElse(new RuleChoice(GateRule.CLIENT_CONFIRMATION, "Client confirmation (default)"));
  }

  /**
   * Records the client's confirmation of an Other Lines account and opens its gate.
   *
   * @param arn Account Reference Number
   * @param confirmation channel, remarks and supporting document
   * @return the evidence
   */
  public PaymentEvidence confirmClient(String arn, ClientConfirmation confirmation) {
    Account account = requireAwaiting(arn);
    if (ruleFor(account).rule() == GateRule.PAYMENT_MATCHED) {
      throw new BusinessRuleException(
          "GATE_REQUIRES_PAYMENT",
          "Account " + arn + " must be paid: match its payment from a payment report");
    }
    lovs.requireValid(CHANNEL_LOV, confirmation.channel(), LocalDate.now(clock));
    PaymentEvidence saved =
        record(
            account,
            EvidenceKind.CLIENT_CONFIRMATION,
            manualOrigin(),
            new EvidenceDetail(
                null,
                null,
                confirmation.channel(),
                confirmation.remarks(),
                confirmation.attachmentId()));
    open(account, saved, "Client confirmation (" + confirmation.channel() + ")");
    return saved;
  }

  /**
   * Releases a direct-payment account still awaiting payment (tagged after validation, BRNB.114):
   * the premium is paid to the insurer, so the gate does not apply.
   *
   * @param arn Account Reference Number
   * @param remarks remarks
   * @return the evidence
   */
  public PaymentEvidence confirmDirect(String arn, String remarks) {
    Account account = requireAwaiting(arn);
    if (!account.isDirectPayment()) {
      throw new BusinessRuleException(
          "GATE_NOT_DIRECT_PAYMENT", "Account " + arn + " is not paid directly to the insurer");
    }
    PaymentEvidence saved =
        record(
            account,
            EvidenceKind.DIRECT_PAYMENT,
            manualOrigin(),
            new EvidenceDetail(null, null, null, remarks, null));
    open(account, saved, "Direct payment to insurer");
    return saved;
  }

  private EvidenceOrigin manualOrigin() {
    return new EvidenceOrigin(
        MANUAL, "CONF-" + currentUser.username() + "-" + clock.instant().toEpochMilli());
  }

  /**
   * Applies a confirmed payment from a source: the evidence is recorded once per source and
   * reference and the gate opens when the account still awaits payment.
   *
   * @param sourceCode source
   * @param payment confirmed payment
   * @return what happened
   */
  public ApplyOutcome applyPayment(String sourceCode, ConfirmedPayment payment) {
    if (evidence.existsByArnAndSourceAndReference(payment.arn(), sourceCode, payment.reference())) {
      return new ApplyOutcome(false, "Already recorded");
    }
    Account account = accounts.require(payment.arn());
    if (account.getStatus() != AccountStatus.AWAITING_PAYMENT) {
      return new ApplyOutcome(
          false, "Account is " + account.getStatus() + ", not awaiting payment");
    }
    PaymentEvidence saved =
        record(
            account,
            EvidenceKind.PAYMENT,
            new EvidenceOrigin(sourceCode, payment.reference()),
            new EvidenceDetail(
                payment.amount(), payment.paidOn(), null, payment.description(), null));
    open(account, saved, payment.description() == null ? sourceCode : payment.description());
    return new ApplyOutcome(true, "Payment gate opened");
  }

  private Account requireAwaiting(String arn) {
    Account account = accounts.require(arn);
    if (account.getStatus() != AccountStatus.AWAITING_PAYMENT) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_AWAITING_PAYMENT",
          "Account " + arn + " is " + account.getStatus() + ", not awaiting payment");
    }
    return account;
  }

  private PaymentEvidence record(
      Account account, EvidenceKind kind, EvidenceOrigin origin, EvidenceDetail detail) {
    return evidence.save(
        new PaymentEvidence(
            new EvidenceAccount(account.getCompanyId(), account.getId(), account.getArn()),
            kind,
            origin,
            detail));
  }

  private void open(Account account, PaymentEvidence saved, String source) {
    String text = source.length() > MAX_SOURCE ? source.substring(0, MAX_SOURCE) : source;
    lifecycle.markPaymentConfirmed(account.getArn(), text);
    saved.markGateOpened();
    audit.record(
        ENTITY,
        account.getArn(),
        AuditAction.UPDATE,
        "Payment gate opened: "
            + saved.getKind()
            + " "
            + saved.getSource()
            + " "
            + saved.getReference());
  }

  /**
   * The client's confirmation.
   *
   * @param channel how the client confirmed (list CLIENT_CONFIRMATION_CHANNEL)
   * @param remarks remarks
   * @param attachmentId supporting document attached to the account, may be null
   */
  public record ClientConfirmation(String channel, String remarks, Long attachmentId) {}

  /**
   * The rule of an account.
   *
   * @param rule rule
   * @param description description
   */
  public record RuleChoice(GateRule rule, String description) {}

  /**
   * Result of applying a confirmed payment.
   *
   * @param opened whether the gate opened
   * @param message outcome
   */
  public record ApplyOutcome(boolean opened, String message) {}

  /**
   * The payment gate of an account.
   *
   * @param account account
   * @param rule rule
   * @param ruleDescription description of the rule
   * @param open whether the gate is open (paid, confirmed or direct)
   * @param evidence evidence, oldest first
   */
  public record GateView(
      Account account,
      GateRule rule,
      String ruleDescription,
      boolean open,
      List<PaymentEvidence> evidence) {

    /** Defensive copy. */
    public GateView {
      evidence = List.copyOf(evidence);
    }
  }
}
