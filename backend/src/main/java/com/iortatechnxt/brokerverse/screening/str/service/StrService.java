package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseAccess;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseCodes;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseMover;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseNotifier;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseTimeline;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate;
import com.iortatechnxt.brokerverse.screening.str.domain.StrField;
import com.iortatechnxt.brokerverse.screening.str.domain.StrFieldRepository;
import com.iortatechnxt.brokerverse.screening.str.domain.StrRepository;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction.Line;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransactionRepository;
import com.iortatechnxt.brokerverse.screening.str.domain.SuspiciousTransactionReport;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prepares STRs from their cases (SNSRP-705; FR-SS-070): the STR is created on the template in
 * force with the subject snapshot, the template fields prefilled from the client, the case and the
 * transactions, and the client's transactions; every field stays editable while it is a draft. The
 * completeness check lists the gaps (mandatory fields, at least one transaction with an amount
 * greater than 0, at least one reason code); Mark Ready moves the case to STR_EXTRACTION and the
 * STR to FOR_APPROVAL, or APPROVED when the AML Committee decided APPROVE_STR.
 */
@Service
@Transactional
public class StrService {

  /** STR reasons (SQ09; demo codes in V1952). */
  static final String REASON_LOV = "SCR_STR_REASON";

  /** Key of the transaction gap. */
  public static final String TRANSACTIONS = "transactions";

  /** Key of the reason gap. */
  public static final String REASONS = "reasonCodes";

  private static final String APPROVE_STR = "APPROVE_STR";

  private final StrRepository strs;
  private final StrFieldRepository fields;
  private final StrTransactionRepository transactions;
  private final ScreeningCaseRepository cases;
  private final ClientService clients;
  private final List<StrTransactionSource> sources;
  private final ActiveConfig config;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final CaseAccess access;
  private final CaseMover mover;
  private final CaseNotifier notifier;
  private final CaseTimeline timeline;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param strs STRs
   * @param fields field values
   * @param transactions transactions
   * @param cases cases
   * @param clients client master (read)
   * @param sources transaction sources
   * @param config active configuration (STR template)
   * @param numbers STR numbers
   * @param lovs lists of values
   * @param access case access
   * @param mover case transitions
   * @param notifier notices
   * @param timeline case timeline
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the STR preparation
  public StrService(
      StrRepository strs,
      StrFieldRepository fields,
      StrTransactionRepository transactions,
      ScreeningCaseRepository cases,
      ClientService clients,
      List<StrTransactionSource> sources,
      ActiveConfig config,
      DocumentNumberService numbers,
      LovService lovs,
      CaseAccess access,
      CaseMover mover,
      CaseNotifier notifier,
      CaseTimeline timeline,
      AuditTrailService audit,
      Clock clock) {
    this.strs = strs;
    this.fields = fields;
    this.transactions = transactions;
    this.cases = cases;
    this.clients = clients;
    this.sources = List.copyOf(sources);
    this.config = config;
    this.numbers = numbers;
    this.lovs = lovs;
    this.access = access;
    this.mover = mover;
    this.notifier = notifier;
    this.timeline = timeline;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The STR of a case, prepared and prefilled on first opening in STR_PREPARATION.
   *
   * @param caseId the case
   * @return the STR
   */
  public SuspiciousTransactionReport prepare(Long caseId) {
    ScreeningCase c = kase(caseId);
    Optional<SuspiciousTransactionReport> existing = strs.findByCaseId(caseId);
    if (existing.isPresent()) {
      return existing.get();
    }
    access.requireActor(c, "prepare the STR", EnumSet.of(CaseStage.STR_PREPARATION));
    LocalDate today = LocalDate.now(clock);
    Client client = clients.get(c.getClientId());
    Optional<ReviewTemplate> template = config.template(c.getCompanyId(), TemplateType.STR, today);
    SuspiciousTransactionReport str =
        strs.save(
            new SuspiciousTransactionReport(
                numbers.next("STR-" + today.getYear()),
                c.getCompanyId(),
                caseId,
                template.map(t -> t.version().id()).orElse(null),
                new SuspiciousTransactionReport.Subject(
                    client.getCode(), client.getDisplayName(), StrPrefill.snapshot(client))));
    List<Line> lines =
        sources.stream().flatMap(s -> s.transactionsOf(client.getId()).stream()).toList();
    lines.forEach(l -> transactions.save(new StrTransaction(str.getId(), l)));
    Map<String, String> prefill = StrPrefill.sources(client, c, lines);
    template.ifPresent(
        t ->
            t.fields().stream()
                .filter(f -> f.prefillSource() != null)
                .forEach(
                    f ->
                        fields.save(
                            new StrField(str.getId(), f.code(), prefill.get(f.prefillSource())))));
    timeline.record(
        c, CaseEventType.STR_PREPARED, EventFacts.change(null, str.getStrNo(), null, null));
    audit.record(
        CaseCodes.ENTITY, c.getCaseNo(), AuditAction.CREATE, "STR " + str.getStrNo() + " prepared");
    return str;
  }

  /**
   * An STR.
   *
   * @param id the STR
   * @return the STR
   */
  @Transactional(readOnly = true)
  public SuspiciousTransactionReport get(Long id) {
    return strs.findById(id).orElseThrow(() -> new ResourceNotFoundException("STR", id));
  }

  /**
   * The STR of a case, if prepared.
   *
   * @param caseId the case
   * @return the STR
   */
  @Transactional(readOnly = true)
  public Optional<SuspiciousTransactionReport> ofCase(Long caseId) {
    return strs.findByCaseId(caseId);
  }

  /**
   * The field values of an STR by code.
   *
   * @param strId the STR
   * @return values
   */
  @Transactional(readOnly = true)
  public Map<String, String> values(Long strId) {
    Map<String, String> values = new LinkedHashMap<>();
    fields.findByStrId(strId).forEach(f -> values.put(f.getFieldCode(), f.getValueText()));
    return values;
  }

  /**
   * The transactions of an STR.
   *
   * @param strId the STR
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<Line> transactions(Long strId) {
    return transactions.findByStrIdOrderByIdAsc(strId).stream().map(StrTransaction::line).toList();
  }

  /**
   * The STR template of an STR.
   *
   * @param str the STR
   * @return the template, empty when none was in force
   */
  @Transactional(readOnly = true)
  public Optional<ReviewTemplate> template(SuspiciousTransactionReport str) {
    return Optional.ofNullable(str.getTemplateVersionId()).map(config::template);
  }

  /**
   * Saves the edits of a draft STR.
   *
   * @param strId the STR
   * @param edit field values, reason codes and the transactions (replacing the list)
   * @return the STR
   */
  public SuspiciousTransactionReport save(Long strId, StrEdit edit) {
    SuspiciousTransactionReport str = get(strId);
    ScreeningCase c = kase(str.getCaseId());
    access.requireActor(c, "edit the STR", EnumSet.of(CaseStage.STR_PREPARATION));
    str.requireDraft();
    validate(edit);
    Set<String> known =
        template(str)
            .map(
                t ->
                    t.fields().stream().map(ReviewTemplate.Field::code).collect(Collectors.toSet()))
            .orElse(Set.of());
    Map<String, StrField> stored =
        fields.findByStrId(strId).stream()
            .collect(Collectors.toMap(StrField::getFieldCode, f -> f));
    edit.values()
        .forEach(
            (code, value) -> {
              if (known.contains(code)) {
                String text = value == null || value.isBlank() ? null : value.strip();
                stored
                    .computeIfAbsent(code, k -> fields.save(new StrField(strId, k, null)))
                    .set(text);
              }
            });
    str.reasons(edit.reasonCodes());
    transactions.deleteAll(transactions.findByStrIdOrderByIdAsc(strId));
    transactions.flush();
    edit.transactions().forEach(l -> transactions.save(new StrTransaction(strId, l)));
    audit.record(
        CaseCodes.ENTITY, c.getCaseNo(), AuditAction.UPDATE, "STR " + str.getStrNo() + " saved");
    return str;
  }

  private void validate(StrEdit edit) {
    Set<String> reasons =
        lovs.activeValues(REASON_LOV, LocalDate.now(clock)).stream()
            .map(LovValue::getCode)
            .collect(Collectors.toSet());
    for (String code : edit.reasonCodes()) {
      if (!reasons.contains(code)) {
        throw new BusinessRuleException(
            "SCR_STR_REASON_INVALID", "'" + code + "' is not an STR reason code");
      }
    }
    for (Line line : edit.transactions()) {
      if (line.amount() == null || line.amount().signum() <= 0) {
        throw new BusinessRuleException(
            "SCR_STR_AMOUNT_INVALID", "The amount must be greater than 0");
      }
      if (blank(line.reference()) || line.date() == null || blank(line.currency())) {
        throw new BusinessRuleException(
            "SCR_STR_TRANSACTION_INCOMPLETE",
            "Enter the reference, date and currency of each transaction");
      }
    }
  }

  /**
   * The gaps of an STR against its template (FR-SS-070 completeness check).
   *
   * @param strId the STR
   * @return messages by field code (and {@value #TRANSACTIONS}, {@value #REASONS}); empty when
   *     complete
   */
  @Transactional(readOnly = true)
  public Map<String, String> gaps(Long strId) {
    SuspiciousTransactionReport str = get(strId);
    Map<String, String> values = values(strId);
    Map<String, String> gaps = new LinkedHashMap<>();
    template(str)
        .ifPresent(
            t ->
                t.fields().stream()
                    .filter(ReviewTemplate.Field::mandatory)
                    .filter(f -> !REASON_LOV.equals(f.lovType()))
                    .filter(f -> blank(values.get(f.code())))
                    .forEach(f -> gaps.put(f.code(), f.label() + " is required for the STR")));
    if (transactions.findByStrIdOrderByIdAsc(strId).isEmpty()) {
      gaps.put(TRANSACTIONS, "Add at least one transaction");
    }
    if (str.reasons().isEmpty()) {
      gaps.put(REASONS, "Select at least one reason code");
    }
    return gaps;
  }

  /**
   * Marks a complete STR ready and moves the case to STR_EXTRACTION.
   *
   * @param strId the STR
   * @return the STR
   */
  public SuspiciousTransactionReport markReady(Long strId) {
    SuspiciousTransactionReport str = get(strId);
    ScreeningCase c = kase(str.getCaseId());
    access.requireActor(c, "mark the STR ready", EnumSet.of(CaseStage.STR_PREPARATION));
    Map<String, String> gaps = gaps(strId);
    if (!gaps.isEmpty()) {
      throw new FieldValidationException(
          "SCR_STR_INCOMPLETE", String.join("; ", gaps.values()), gaps);
    }
    str.ready(
        access.user(),
        clock.instant(),
        APPROVE_STR.equals(c.getCommitteeDecision()) ? c.getCommitteeDecidedAt() : null);
    mover.act(
        c, "str_ready", TransitionNote.comment("STR " + str.getStrNo() + " " + str.getStatus()));
    timeline.record(
        c,
        CaseEventType.STR_READY,
        EventFacts.move(
            CaseStage.STR_PREPARATION.name(),
            CaseStage.STR_EXTRACTION.name(),
            null,
            str.getStrNo() + " " + str.getStatus()));
    mover.assign(
        c, null, CaseEventType.ASSIGNED, CaseMover.AssignFacts.auto("STR extraction queue"));
    notifier.owner(
        c,
        CaseCodes.STR_EXTRACT,
        CaseCodes.EVENT_FOR_APPROVAL,
        "STR " + str.getStrNo() + " ready for extraction");
    audit.record(
        CaseCodes.ENTITY,
        c.getCaseNo(),
        AuditAction.SUBMIT,
        "STR " + str.getStrNo() + " " + str.getStatus());
    return str;
  }

  private ScreeningCase kase(Long caseId) {
    return cases
        .findById(caseId)
        .orElseThrow(() -> new ResourceNotFoundException(CaseCodes.ENTITY, caseId));
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }

  /**
   * The edits of a draft STR.
   *
   * @param values template field values by code (fields not given keep their value)
   * @param reasonCodes the reason codes
   * @param transactions the transactions (replace the list)
   */
  public record StrEdit(
      Map<String, String> values, Set<String> reasonCodes, List<Line> transactions) {

    /** Defensive copies; nulls are empty. */
    public StrEdit {
      values = values == null ? Map.of() : new LinkedHashMap<>(values);
      reasonCodes = reasonCodes == null ? Set.of() : Set.copyOf(reasonCodes);
      transactions = transactions == null ? List.of() : List.copyOf(transactions);
    }
  }
}
