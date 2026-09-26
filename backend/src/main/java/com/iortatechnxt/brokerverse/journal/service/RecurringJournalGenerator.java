package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalHeader;
import com.iortatechnxt.brokerverse.journal.domain.RecurringJournalTemplate;
import com.iortatechnxt.brokerverse.journal.domain.RecurringJournalTemplateRepository;
import com.iortatechnxt.brokerverse.journal.domain.RecurringLine;
import com.iortatechnxt.brokerverse.journal.domain.RecurringOccurrence;
import com.iortatechnxt.brokerverse.journal.domain.RecurringOccurrenceRepository;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Generates the journal of one recurring template occurrence, each in its own transaction so a
 * failing occurrence never affects others. Idempotent: an occurrence already recorded is skipped.
 */
@Component
public class RecurringJournalGenerator {

  /** Source module recorded on generated journals. */
  public static final String SOURCE_MODULE = "RECURRING";

  private final RecurringJournalTemplateRepository templates;
  private final RecurringOccurrenceRepository occurrences;
  private final JournalBatchRepository batches;
  private final JournalFactory factory;
  private final JournalValidator validator;
  private final UserDirectory userDirectory;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final TransactionTemplate newTransaction;

  /**
   * Creates the generator.
   *
   * @param templates template repository
   * @param occurrences occurrence repository
   * @param batches batch repository
   * @param factory journal factory
   * @param validator journal validator
   * @param userDirectory user facts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param transactionManager transaction manager
   */
  public RecurringJournalGenerator(
      RecurringJournalTemplateRepository templates,
      RecurringOccurrenceRepository occurrences,
      JournalBatchRepository batches,
      JournalFactory factory,
      JournalValidator validator,
      UserDirectory userDirectory,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    this.templates = templates;
    this.occurrences = occurrences;
    this.batches = batches;
    this.factory = factory;
    this.validator = validator;
    this.userDirectory = userDirectory;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Occurrences of a template due up to a date (read in its own transaction).
   *
   * @param templateId template
   * @param asOf run date
   * @return due dates
   */
  public List<LocalDate> dueOccurrences(Long templateId, LocalDate asOf) {
    return templates.findById(templateId).map(t -> t.dueOccurrences(asOf)).orElse(List.of());
  }

  /**
   * Generates the draft journal (and the reversing draft for auto-reverse templates) of one
   * occurrence and records the occurrence.
   *
   * @param templateId template
   * @param date occurrence date
   * @return generated journal, empty when the occurrence already exists
   */
  public Optional<GeneratedJournal> generate(Long templateId, LocalDate date) {
    return Optional.ofNullable(newTransaction.execute(s -> doGenerate(templateId, date)));
  }

  /**
   * Validates and submits a generated journal for approval. On failure the journal stays a draft.
   *
   * @param generated generated journal
   * @return the journal with the submission outcome
   */
  public GeneratedJournal submit(GeneratedJournal generated) {
    try {
      newTransaction.executeWithoutResult(
          s -> {
            JournalBatch batch = batches.findById(generated.batchId()).orElseThrow();
            String user = currentUser.username();
            validator.validate(batch, userDirectory.roleCodes(user));
            batch.submit(user, clock.instant());
            audit.record(
                JournalEntryService.ENTITY,
                batch.getBatchNo(),
                AuditAction.SUBMIT,
                "Submitted automatically (recurring template " + generated.templateName() + ")");
          });
      return generated.withSubmission(true, null);
    } catch (BusinessRuleException ex) {
      return generated.withSubmission(false, "Left as draft: " + ex.getMessage());
    }
  }

  private GeneratedJournal doGenerate(Long templateId, LocalDate date) {
    if (occurrences.existsByTemplateIdAndOccurrenceDate(templateId, date)) {
      return null;
    }
    RecurringJournalTemplate t = templates.findById(templateId).orElseThrow();
    String key = "RJ-" + templateId + "-" + date;
    JournalBatch batch =
        factory.create(header(t, date, t.getNarration(), key), lines(t, RecurringLines::toRequest));
    JournalBatch reversal = null;
    if (t.isAutoReverse()) {
      LocalDate nextPeriod = date.withDayOfMonth(date.lengthOfMonth()).plusDays(1);
      String narration = "Reversal of accrual " + batch.getBatchNo() + ": " + t.getNarration();
      reversal =
          factory.create(
              header(t, nextPeriod, narration, key + "-R"),
              lines(t, RecurringLines::toReversedRequest));
    }
    String user = currentUser.username();
    occurrences.save(
        new RecurringOccurrence(
            templateId,
            date,
            batch.getId(),
            reversal == null ? null : reversal.getId(),
            clock.instant(),
            user));
    t.markGenerated(date);
    recordAudit(batch, t, date);
    if (reversal != null) {
      recordAudit(reversal, t, date);
    }
    return new GeneratedJournal(
        templateId,
        t.getName(),
        date,
        batch.getId(),
        batch.getBatchNo(),
        reversal == null ? null : reversal.getBatchNo(),
        false,
        null);
  }

  private void recordAudit(JournalBatch batch, RecurringJournalTemplate t, LocalDate date) {
    audit.record(
        JournalEntryService.ENTITY,
        batch.getBatchNo(),
        AuditAction.CREATE,
        "Generated from recurring template " + t.getName() + " for " + date);
  }

  private JournalHeader header(
      RecurringJournalTemplate t, LocalDate valueDate, String narration, String sourceKey) {
    return new JournalHeader(
        t.getCompanyId(),
        t.getBranchId(),
        t.getJournalType(),
        LocalDate.now(clock),
        valueDate,
        t.getCurrency(),
        narration,
        t.getReference(),
        SOURCE_MODULE,
        sourceKey,
        null);
  }

  private static List<JournalLineRequest> lines(
      RecurringJournalTemplate t, Function<RecurringLine, JournalLineRequest> mapper) {
    return t.getLines().stream().map(mapper).toList();
  }
}
