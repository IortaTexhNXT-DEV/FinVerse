package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalHeader;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.journal.domain.JournalSearchCriteria;
import com.iortatechnxt.brokerverse.journal.domain.JournalSpecifications;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
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
    return createDraft(request, null, null);
  }

  /**
   * Creates a manual journal in DRAFT that records where it came from, e.g. {@code JOURNAL_UPLOAD}
   * and the upload reference. The source is kept when the draft is edited.
   *
   * @param request request
   * @param sourceModule originating function, null for keyed entry
   * @param sourceReference reference within the source (e.g. upload batch), may be null
   * @return draft batch
   */
  public JournalBatch createDraft(
      JournalRequest request, String sourceModule, String sourceReference) {
    JournalBatch saved =
        factory.create(manualHeader(request, sourceModule, sourceReference), request.lines());
    saved.scheduleReversal(request.reverseOn());
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
    // Delete the old lines first: Hibernate would otherwise insert the new rows before removing the
    // orphans and violate the (batch_id, line_no) unique key.
    batch.replaceLines(List.of());
    batches.flush();
    batch.replaceLines(factory.resolve(header, request.lines()));
    batch.scheduleReversal(request.reverseOn());
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
   * Assigns unposted journals to the user who will post them, or clears the assignment (FRBS
   * 2.5.1). The assignee must hold the journal authorization permission and must not be the
   * journal's submitter.
   *
   * @param ids journals
   * @param assignee user name, null or blank to clear
   * @return assigned journals
   */
  public List<JournalBatch> assign(List<Long> ids, String assignee) {
    String user = assignee == null || assignee.isBlank() ? null : assignee.trim();
    if (user != null
        && userDirectory.usersWithPermission("JOURNAL_AUTHORIZE").stream()
            .noneMatch(user::equalsIgnoreCase)) {
      throw new BusinessRuleException(
          "ASSIGNEE_NOT_AUTHORIZER", user + " is not allowed to authorize journals");
    }
    return ids.stream().map(id -> assignOne(get(id), user)).toList();
  }

  private JournalBatch assignOne(JournalBatch batch, String user) {
    if (CurrentUser.sameUser(user, batch.getSubmittedBy())) {
      throw new BusinessRuleException(
          "ASSIGNEE_IS_MAKER",
          batch.getBatchNo() + " cannot be assigned to the user who submitted it");
    }
    batch.assign(user, currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        batch.getBatchNo(),
        AuditAction.UPDATE,
        user == null ? "Assignment cleared" : "Assigned to " + user);
    return batch;
  }

  /**
   * Non-blocking warnings of a journal (FRBS 2.5.5 / 2.8.4), for the confirmation before submit and
   * approve (FRBS 2.5.10 / 2.8.3).
   *
   * @param id journal
   * @return warnings
   */
  @Transactional(readOnly = true)
  public List<String> warnings(Long id) {
    return validator.warnings(get(id));
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
    return manualHeader(r, null, null);
  }

  private JournalHeader manualHeader(JournalRequest r, String sourceModule, String sourceRef) {
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
        sourceModule,
        sourceRef,
        null);
  }
}
