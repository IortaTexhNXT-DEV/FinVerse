package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.payrequest.domain.ExpenseValues;
import com.iortatechnxt.brokerverse.payrequest.domain.Liquidation;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationLine;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationStatus;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liquidation of a disbursed cash advance (Appendix D Cash Advance Liquidation Form; in scope if
 * confirmed, AQ18): the employee enters the fieldwork days and expenses and submits; a reviewer who
 * is not the employee checks and posts it with event {@code PRQ_CA_LIQUIDATION} (expenses by
 * category against the advance, the excess returned or the shortage payable to the employee). The
 * expense and cash accounts of the event roles are configuration ({@link LiquidationAccounts}).
 */
@Service
@Transactional
public class LiquidationService {

  /** Event type of the liquidation (V890). */
  public static final String EVENT = "PRQ_CA_LIQUIDATION";

  private static final String ENTITY = "Liquidation";
  private static final int MAX_LINES = 60;

  private final LiquidationRepository liquidations;
  private final LiquidationAccounts accounts;
  private final PaymentRequestRepository requests;
  private final AccountingEventPublisher accounting;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param liquidations liquidations
   * @param accounts accounts of the event roles
   * @param requests requests
   * @param accounting accounting engine
   * @param numbers liquidation numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public LiquidationService(
      LiquidationRepository liquidations,
      LiquidationAccounts accounts,
      PaymentRequestRepository requests,
      AccountingEventPublisher accounting,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.liquidations = liquidations;
    this.accounts = accounts;
    this.requests = requests;
    this.accounting = accounting;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The liquidation of a cash advance, if started.
   *
   * @param requestId cash-advance request
   * @return liquidation
   */
  @Transactional(readOnly = true)
  public Optional<Liquidation> of(Long requestId) {
    return liquidations.findByRequestId(requestId);
  }

  /**
   * Starts or changes the liquidation of a disbursed cash advance.
   *
   * @param requestId cash-advance request
   * @param draft form content
   * @return the liquidation
   */
  public Liquidation save(Long requestId, Draft draft) {
    PaymentRequest advance = disbursedAdvance(requestId);
    Liquidation liquidation =
        liquidations
            .findByRequestId(requestId)
            .orElseGet(
                () ->
                    liquidations.save(
                        new Liquidation(
                            requestId,
                            numbers.next("LIQ-" + LocalDate.now(clock).getYear()),
                            advance.getAmount())));
    if (liquidation.getStatus() != LiquidationStatus.DRAFT) {
      throw new BusinessRuleException(
          "PRQ_LIQUIDATION_LOCKED",
          liquidation.getLiquidationNo() + " is " + liquidation.getStatus());
    }
    liquidation.fill(
        trim(draft.jobLevel()),
        trim(draft.costCenter()),
        trim(draft.remarks()),
        lines(draft.days()));
    audit.record(
        ENTITY,
        liquidation.getLiquidationNo(),
        AuditAction.UPDATE,
        "Expenses " + liquidation.getTotalExpenses().toPlainString());
    return liquidation;
  }

  /**
   * Submits the liquidation for checking.
   *
   * @param requestId cash-advance request
   * @return the liquidation
   */
  public Liquidation submit(Long requestId) {
    Liquidation liquidation = require(requestId, LiquidationStatus.DRAFT);
    if (liquidation.getLines().isEmpty()) {
      throw new BusinessRuleException(
          "PRQ_LIQUIDATION_EMPTY", "Enter at least one fieldwork day before submitting");
    }
    liquidation.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, liquidation.getLiquidationNo(), AuditAction.SUBMIT, "Submitted");
    return liquidation;
  }

  /**
   * Returns a submitted liquidation to the employee.
   *
   * @param requestId cash-advance request
   * @param reason reason
   * @return the liquidation
   */
  public Liquidation sendBack(Long requestId, String reason) {
    Liquidation liquidation = require(requestId, LiquidationStatus.SUBMITTED);
    liquidation.sendBack(trim(reason));
    audit.record(ENTITY, liquidation.getLiquidationNo(), AuditAction.REJECT, "Returned: " + reason);
    return liquidation;
  }

  /**
   * Checks and posts a submitted liquidation (four eyes: not by its submitter).
   *
   * @param requestId cash-advance request
   * @return the liquidation
   */
  public Liquidation post(Long requestId) {
    Liquidation liquidation = require(requestId, LiquidationStatus.SUBMITTED);
    if (CurrentUser.sameUser(currentUser.username(), liquidation.getSubmittedBy())) {
      throw new BusinessRuleException(
          PayRequests.FOUR_EYES, "A liquidation is checked by someone other than the employee");
    }
    PaymentRequest advance = disbursedAdvance(requestId);
    Map<String, BigDecimal> amounts = amounts(liquidation);
    JournalBatch batch =
        accounting.publish(
            new BusinessEvent(
                EVENT,
                advance.getCompanyId(),
                advance.getBranchId(),
                LocalDate.now(clock),
                advance.getContent().currency(),
                PayRequests.MODULE,
                "LIQ:" + liquidation.getLiquidationNo(),
                liquidation.getLiquidationNo(),
                advance.getPayee().code(),
                null,
                liquidation.getCostCenter(),
                "Liquidation of cash advance " + advance.getRequestNo(),
                amounts,
                accounts.forAmounts(advance.getCompanyId(), amounts),
                Map.of()));
    liquidation.posted(currentUser.username(), clock.instant(), batch.getBatchNo());
    audit.record(
        ENTITY, liquidation.getLiquidationNo(), AuditAction.POST, "Posted " + batch.getBatchNo());
    return liquidation;
  }

  private static Map<String, BigDecimal> amounts(Liquidation l) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    put(amounts, "PER_DIEM", l.sum(LiquidationLine::getPerDiem));
    put(amounts, "REPRESENTATION", l.sum(LiquidationLine::getRepresentation));
    put(amounts, "TRANSPORT", l.sum(LiquidationLine::getTransport));
    put(amounts, "LODGING", l.sum(LiquidationLine::getLodging));
    put(amounts, "OTHER", l.sum(LiquidationLine::getOthers));
    BigDecimal overShort = l.getOverShort();
    put(amounts, "CASH_RETURNED", overShort.max(BigDecimal.ZERO));
    put(amounts, "SHORTAGE", overShort.negate().max(BigDecimal.ZERO));
    put(amounts, "ADVANCE", l.getCashAdvanced());
    return amounts;
  }

  private static void put(Map<String, BigDecimal> amounts, String key, BigDecimal value) {
    if (value.signum() != 0) {
      amounts.put(key, value);
    }
  }

  private List<LiquidationLine> lines(List<ExpenseValues> days) {
    if (days == null || days.size() > MAX_LINES) {
      throw new BusinessRuleException(
          "PRQ_LIQUIDATION_LINES", "A liquidation has up to " + MAX_LINES + " fieldwork days");
    }
    List<LiquidationLine> lines = new ArrayList<>();
    int no = 1;
    for (ExpenseValues day : days) {
      lines.add(new LiquidationLine(no++, checked(day)));
    }
    return lines;
  }

  private static ExpenseValues checked(ExpenseValues day) {
    if (day.fieldworkDate() == null || day.particulars() == null || day.particulars().isBlank()) {
      throw new BusinessRuleException(
          "PRQ_LIQUIDATION_LINE", "Every fieldwork day needs its date and particulars");
    }
    return new ExpenseValues(
        day.fieldworkDate(),
        day.particulars().strip(),
        money(day.perDiem()),
        money(day.representation()),
        money(day.transport()),
        money(day.lodging()),
        money(day.others()));
  }

  private static BigDecimal money(BigDecimal value) {
    BigDecimal v = value == null ? BigDecimal.ZERO : value;
    if (v.signum() < 0 || v.scale() > 2) {
      throw new BusinessRuleException(
          "PRQ_AMOUNT_INVALID", "Expenses are zero or positive with at most two decimals");
    }
    return v.setScale(2);
  }

  private PaymentRequest disbursedAdvance(Long requestId) {
    PaymentRequest request =
        requests
            .findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(PayRequests.ENTITY, requestId));
    if (request.getKind() != RequestKind.CASH_ADVANCE
        || request.getStage() != RequestStage.DISBURSED) {
      throw new BusinessRuleException(
          "PRQ_NOT_LIQUIDABLE", "Only a disbursed cash advance is liquidated");
    }
    return request;
  }

  private Liquidation require(Long requestId, LiquidationStatus status) {
    Liquidation liquidation =
        liquidations
            .findByRequestId(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(ENTITY, requestId));
    if (liquidation.getStatus() != status) {
      throw new BusinessRuleException(
          "PRQ_LIQUIDATION_LOCKED",
          liquidation.getLiquidationNo() + " is " + liquidation.getStatus());
    }
    return liquidation;
  }

  private static String trim(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * The liquidation form.
   *
   * @param jobLevel job level
   * @param costCenter cost centre of the expenses
   * @param remarks remarks
   * @param days fieldwork days
   */
  public record Draft(
      String jobLevel, String costCenter, String remarks, List<ExpenseValues> days) {

    /** Defensive copy. */
    public Draft {
      days = days == null ? List.of() : List.copyOf(days);
    }
  }
}
