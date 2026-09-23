package com.iortatechnxt.finverse.journal.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.journal.api.dto.RecurringTemplateRequest;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.finverse.journal.domain.JournalHeader;
import com.iortatechnxt.finverse.journal.domain.RecurringJournalTemplate;
import com.iortatechnxt.finverse.journal.domain.RecurringJournalTemplate.RecurringHeader;
import com.iortatechnxt.finverse.journal.domain.RecurringJournalTemplate.RecurringSchedule;
import com.iortatechnxt.finverse.journal.domain.RecurringJournalTemplateRepository;
import com.iortatechnxt.finverse.journal.domain.RecurringLine;
import com.iortatechnxt.finverse.journal.domain.RecurringOccurrence;
import com.iortatechnxt.finverse.journal.domain.RecurringOccurrenceRepository;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import com.iortatechnxt.finverse.system.service.JobOutcome;
import com.iortatechnxt.finverse.system.service.JobRunService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recurring journal templates: maintenance, generation history and generation runs. Runs are
 * recorded in the job monitor under {@value #JOB_NAME}; each occurrence is generated in its own
 * transaction by {@link RecurringJournalGenerator}.
 */
@Service
@Transactional
public class RecurringJournalService {

  /** Job monitor name of recurring journal generation. */
  public static final String JOB_NAME = "RECURRING_JOURNALS";

  private static final String ENTITY = "RecurringJournalTemplate";

  private final RecurringJournalTemplateRepository templates;
  private final RecurringOccurrenceRepository occurrences;
  private final JournalBatchRepository batches;
  private final RecurringJournalGenerator generator;
  private final JournalFactory factory;
  private final JobRunService jobRuns;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param templates template repository
   * @param occurrences occurrence repository
   * @param batches batch repository
   * @param generator occurrence generator
   * @param factory journal factory (account validation)
   * @param jobRuns job run recorder
   * @param audit audit trail
   * @param clock clock
   */
  public RecurringJournalService(
      RecurringJournalTemplateRepository templates,
      RecurringOccurrenceRepository occurrences,
      JournalBatchRepository batches,
      RecurringJournalGenerator generator,
      JournalFactory factory,
      JobRunService jobRuns,
      AuditTrailService audit,
      Clock clock) {
    this.templates = templates;
    this.occurrences = occurrences;
    this.batches = batches;
    this.generator = generator;
    this.factory = factory;
    this.jobRuns = jobRuns;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Templates of a company.
   *
   * @param companyId company
   * @return templates
   */
  @Transactional(readOnly = true)
  public List<RecurringJournalTemplate> list(Long companyId) {
    return templates.findByCompanyIdOrderByName(companyId);
  }

  /**
   * Gets a template.
   *
   * @param id id
   * @return template
   */
  @Transactional(readOnly = true)
  public RecurringJournalTemplate get(Long id) {
    return templates
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Recurring journal template", id));
  }

  /**
   * Creates a template after checking balance and accounts.
   *
   * @param request request
   * @return template
   */
  public RecurringJournalTemplate create(RecurringTemplateRequest request) {
    if (templates.existsByCompanyIdAndNameIgnoreCase(request.companyId(), request.name())) {
      throw new DuplicateResourceException("Recurring journal template", request.name());
    }
    validate(request);
    RecurringJournalTemplate saved =
        templates.save(
            new RecurringJournalTemplate(
                request.companyId(), header(request), schedule(request), lines(request)));
    audit.record(ENTITY, saved.getName(), AuditAction.CREATE, "Created recurring template");
    return saved;
  }

  /**
   * Updates a template; occurrences already generated are unaffected.
   *
   * @param id id
   * @param request request
   * @return template
   */
  public RecurringJournalTemplate update(Long id, RecurringTemplateRequest request) {
    RecurringJournalTemplate template = get(id);
    if (!template.getCompanyId().equals(request.companyId())) {
      throw new BusinessRuleException("COMPANY_MISMATCH", "A template cannot change company");
    }
    validate(request);
    template.apply(header(request), schedule(request), lines(request));
    audit.record(ENTITY, template.getName(), AuditAction.UPDATE, "Updated recurring template");
    return template;
  }

  /**
   * Activates or deactivates a template.
   *
   * @param id id
   * @param active new state
   * @return template
   */
  public RecurringJournalTemplate setActive(Long id, boolean active) {
    RecurringJournalTemplate template = get(id);
    template.setActive(active);
    audit.record(
        ENTITY,
        template.getName(),
        active ? AuditAction.UPDATE : AuditAction.DEACTIVATE,
        active ? "Activated recurring template" : "Deactivated recurring template");
    return template;
  }

  /**
   * Generation history of a template.
   *
   * @param id template
   * @return occurrences with their journals, newest first
   */
  @Transactional(readOnly = true)
  public List<OccurrenceView> history(Long id) {
    List<RecurringOccurrence> list = occurrences.findByTemplateIdOrderByOccurrenceDateDesc(id);
    List<Long> ids =
        list.stream()
            .flatMap(o -> Stream.of(o.getBatchId(), o.getReversalBatchId()))
            .filter(Objects::nonNull)
            .toList();
    Map<Long, JournalBatch> journals =
        batches.findAllById(ids).stream()
            .collect(Collectors.toMap(JournalBatch::getId, Function.identity()));
    return list.stream().map(o -> OccurrenceView.of(o, journals)).toList();
  }

  /**
   * Generates every due occurrence of every active template (scheduled job body).
   *
   * @param asOf run date
   * @return result
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public GenerationResult generateDue(LocalDate asOf) {
    return generate(
        templates.findByActiveTrueOrderById().stream()
            .map(RecurringJournalTemplate::getId)
            .toList(),
        asOf);
  }

  /**
   * Generates the due occurrences of one template now ("run now"); recorded in the job monitor.
   *
   * @param id template
   * @param asOf run date
   * @return result
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public GenerationResult runTemplate(Long id, LocalDate asOf) {
    RecurringJournalTemplate template = get(id);
    if (!template.isActive()) {
      throw new BusinessRuleException("TEMPLATE_INACTIVE", "The template is not active");
    }
    AtomicReference<GenerationResult> result = new AtomicReference<>();
    jobRuns.execute(
        JOB_NAME,
        JobTrigger.MANUAL,
        () -> outcome(result, generate(List.of(id), asOf), template.getName()));
    GenerationResult outcome = result.get();
    return outcome != null
        ? outcome
        : new GenerationResult(List.of(), List.of("Generation run failed, see the job monitor"));
  }

  /**
   * Converts a generation result into a job outcome; failed occurrences fail the job run so the
   * JOB_FAILURE alert is raised.
   *
   * @param holder receives the result
   * @param result result
   * @param scope description of what was run
   * @return outcome
   */
  static JobOutcome outcome(
      AtomicReference<GenerationResult> holder, GenerationResult result, String scope) {
    holder.set(result);
    if (!result.errors().isEmpty()) {
      throw new BusinessRuleException("RECURRING_GENERATION_FAILED", result.message());
    }
    return new JobOutcome(result.generated(), scope + ": " + result.message());
  }

  private GenerationResult generate(List<Long> templateIds, LocalDate asOf) {
    List<GeneratedJournal> generated = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    for (Long id : templateIds) {
      for (LocalDate date : generator.dueOccurrences(id, asOf)) {
        try {
          generator.generate(id, date).map(this::submitIfConfigured).ifPresent(generated::add);
        } catch (RuntimeException ex) {
          // Later occurrences wait until this one succeeds, keeping the series in order.
          errors.add("template " + id + " on " + date + ": " + ex.getMessage());
          break;
        }
      }
    }
    return new GenerationResult(generated, errors);
  }

  private GeneratedJournal submitIfConfigured(GeneratedJournal journal) {
    boolean autoSubmit =
        templates
            .findById(journal.templateId())
            .map(RecurringJournalTemplate::isAutoSubmit)
            .orElse(false);
    return autoSubmit ? generator.submit(journal) : journal;
  }

  private void validate(RecurringTemplateRequest r) {
    RecurringLines.requireBalanced(r.currency(), r.lines());
    // Resolves every line (account, branch, currency) exactly as generation will.
    factory.resolve(
        new JournalHeader(
            r.companyId(),
            r.branchId(),
            r.journalType(),
            LocalDate.now(clock),
            r.startDate(),
            r.currency(),
            r.narration(),
            r.reference(),
            null,
            null,
            null),
        r.lines());
  }

  private static RecurringHeader header(RecurringTemplateRequest r) {
    return new RecurringHeader(
        r.branchId(),
        r.name().strip(),
        r.journalType(),
        r.currency(),
        r.narration(),
        r.reference(),
        r.autoReverse(),
        r.autoSubmit());
  }

  private static RecurringSchedule schedule(RecurringTemplateRequest r) {
    return new RecurringSchedule(r.frequency(), r.dayOfMonth(), r.startDate(), r.endDate());
  }

  private static List<RecurringLine> lines(RecurringTemplateRequest r) {
    return r.lines().stream().map(RecurringLines::fromRequest).toList();
  }

  /**
   * Generated occurrence with its journals.
   *
   * @param occurrenceDate occurrence date
   * @param batchId journal id
   * @param batchNo journal number
   * @param batchStatus journal status
   * @param reversalBatchId reversing journal id
   * @param reversalBatchNo reversing journal number
   * @param generatedAt generation time
   * @param generatedBy user or SYSTEM
   */
  public record OccurrenceView(
      LocalDate occurrenceDate,
      Long batchId,
      String batchNo,
      String batchStatus,
      Long reversalBatchId,
      String reversalBatchNo,
      Instant generatedAt,
      String generatedBy) {

    static OccurrenceView of(RecurringOccurrence o, Map<Long, JournalBatch> journals) {
      JournalBatch batch = journals.get(o.getBatchId());
      JournalBatch reversal =
          o.getReversalBatchId() == null ? null : journals.get(o.getReversalBatchId());
      return new OccurrenceView(
          o.getOccurrenceDate(),
          o.getBatchId(),
          batch == null ? null : batch.getBatchNo(),
          batch == null ? null : batch.getStatus().name(),
          o.getReversalBatchId(),
          reversal == null ? null : reversal.getBatchNo(),
          o.getGeneratedAt(),
          o.getGeneratedBy());
    }
  }
}
