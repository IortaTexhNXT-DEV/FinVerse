package com.iortatechnxt.finverse.journal.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.finverse.journal.domain.JournalHeader;
import com.iortatechnxt.finverse.journal.domain.JournalLine;
import com.iortatechnxt.finverse.journal.domain.JournalSearchCriteria;
import com.iortatechnxt.finverse.journal.domain.JournalSpecifications;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maker side of manual journals: inquiry, draft entry and update, submission for authorization,
 * cancellation and the copy-transaction facility.
 */
@Service
@Transactional
public class JournalEntryService {

  static final String ENTITY = "JournalBatch";
  private static final Set<JournalType> MANUAL_TYPES =
      EnumSet.of(JournalType.MANUAL, JournalType.ADJUSTMENT, JournalType.ACCRUAL);

  private final JournalBatchRepository batches;
  private final JournalFactory factory;
  private final JournalValidator validator;
  private final UserDirectory userDirectory;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches batch repository
   * @param factory journal factory
   * @param validator validator
   * @param userDirectory user facts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public JournalEntryService(
      JournalBatchRepository batches,
      JournalFactory factory,
      JournalValidator validator,
      UserDirectory userDirectory,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.batches = batches;
    this.factory = factory;
    this.validator = validator;
    this.userDirectory = userDirectory;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Searches journals (inquiry and transaction checklist).
   *
   * @param criteria filters
   * @param pageable paging
   * @return page of batches
   */
  @Transactional(readOnly = true)
  public Page<JournalBatch> search(JournalSearchCriteria criteria, Pageable pageable) {
    return batches.findAll(JournalSpecifications.matching(criteria), pageable);
  }

  /**
   * Gets a batch.
   *
   * @param id id
   * @return batch
   */
  @Transactional(readOnly = true)
  public JournalBatch get(Long id) {
    return batches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Journal", id));
  }

  /**
   * Creates a manual journal in DRAFT. Drafts may be incomplete or unbalanced.
   *
   * @param request request
   * @return draft batch
   */
  public JournalBatch createDraft(JournalRequest request) {
    JournalBatch saved = factory.create(manualHeader(request), request.lines());
    audit.record(
        ENTITY,
        saved.getBatchNo(),
        AuditAction.CREATE,
        "Created " + saved.getJournalType() + " journal");
    return saved;
  }

  /**
   * Updates a DRAFT or REJECTED journal (maker only).
   *
   * @param id id
   * @param request request
   * @return updated batch
   */
  public JournalBatch update(Long id, JournalRequest request) {
    JournalBatch batch = get(id);
    requireMaker(batch);
    JournalHeader header = manualHeader(request);
    batch.updateHeader(header);
    batch.replaceLines(factory.resolve(header, request.lines()));
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.UPDATE, "Updated journal");
    return batch;
  }

  /**
   * Submits a journal for authorization after full validation.
   *
   * @param id id
   * @return batch pending approval
   */
  public JournalBatch submit(Long id) {
    JournalBatch batch = get(id);
    String user = currentUser.username();
    validator.validate(batch, userDirectory.roleCodes(user));
    batch.submit(user, clock.instant());
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.SUBMIT, "Submitted for authorization");
    return batch;
  }

  /**
   * Cancels an unposted journal (maker only). Cancelled journals are retained for audit.
   *
   * @param id id
   * @return cancelled batch
   */
  public JournalBatch cancel(Long id) {
    JournalBatch batch = get(id);
    requireMaker(batch);
    batch.cancel();
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.DEACTIVATE, "Cancelled journal");
    return batch;
  }

  /**
   * Copies a journal into a new DRAFT (copy-transaction facility).
   *
   * @param id source batch
   * @param valueDate value date of the copy
   * @return new draft
   */
  public JournalBatch copy(Long id, LocalDate valueDate) {
    JournalBatch source = get(id);
    JournalType type =
        MANUAL_TYPES.contains(source.getJournalType())
            ? source.getJournalType()
            : JournalType.MANUAL;
    JournalHeader header =
        new JournalHeader(
            source.getCompanyId(),
            source.getBranchId(),
            type,
            LocalDate.now(clock),
            valueDate,
            source.getCurrency(),
            source.getNarration(),
            source.getReference(),
            null,
            null,
            null);
    JournalBatch saved =
        factory.createFromSpecs(
            header, source.getLines().stream().map(JournalLine::toSpec).toList());
    audit.record(
        ENTITY, saved.getBatchNo(), AuditAction.CREATE, "Copied from " + source.getBatchNo());
    return saved;
  }

  private void requireMaker(JournalBatch batch) {
    if (!Objects.equals(currentUser.username(), batch.getCreatedBy())) {
      throw new BusinessRuleException(
          "NOT_MAKER", "Only the maker (" + batch.getCreatedBy() + ") can change this journal");
    }
  }

  private JournalHeader manualHeader(JournalRequest r) {
    if (!MANUAL_TYPES.contains(r.journalType())) {
      throw new BusinessRuleException(
          "INVALID_JOURNAL_TYPE",
          "Only MANUAL, ADJUSTMENT or ACCRUAL journals can be entered manually");
    }
    return new JournalHeader(
        r.companyId(),
        r.branchId(),
        r.journalType(),
        LocalDate.now(clock),
        r.valueDate(),
        r.currency(),
        r.narration(),
        r.reference(),
        null,
        null,
        null);
  }
}
