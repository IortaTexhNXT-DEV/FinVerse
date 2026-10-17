package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.csf.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore.Filter;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore.Note;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.CaseRecord;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.CaseRow;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.Linked;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the case screens (FR-CSF-052 to 060): the work queues, the case record with its notes,
 * documents, linked cases and history, and the cases of a client. A member of a fulfilment unit
 * without the CSF roles sees the cases of his units only.
 */
@Service
@Transactional(readOnly = true)
public class CsfCaseQueryService {

  private static final int MAX_ROWS = 500;

  private final CsfCaseStore cases;
  private final CsfCaseSetupStore setup;
  private final CsfCaseAccess service;
  private final CsfCaseViews views;
  private final DocumentService documents;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param setup fulfilment units of the user
   * @param service access and actions
   * @param views answers
   * @param documents case documents
   * @param audit case history
   * @param currentUser signed-in user
   */
  public CsfCaseQueryService(
      CsfCaseStore cases,
      CsfCaseSetupStore setup,
      CsfCaseAccess service,
      CsfCaseViews views,
      DocumentService documents,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.cases = cases;
    this.setup = setup;
    this.service = service;
    this.views = views;
    this.documents = documents;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /** The work queues of the Cases screen. */
  public enum Queue {
    /** Open cases the user owns. */
    MINE,
    /** Cases returned to the user. */
    RETURNED,
    /** Cases waiting for the user's action with the client. */
    CSR_ACTION,
    /** Open cases of the team (supervisors). */
    TEAM,
    /** Cases with the user's fulfilment units. */
    UNIT,
    /** Every open case. */
    OPEN,
    /** Closed cases. */
    CLOSED,
    /** Every case. */
    ALL
  }

  /**
   * A note of a case.
   *
   * @param note text
   * @param author user
   * @param createdAt time
   */
  public record NoteView(String note, String author, Instant createdAt) {}

  /**
   * An entry of the case history.
   *
   * @param at time
   * @param user user
   * @param action action
   * @param summary what was done
   * @param from value before
   * @param to value after
   */
  public record HistoryView(
      Instant at, String user, String action, String summary, String from, String to) {}

  /**
   * A document of a case.
   *
   * @param id attachment
   * @param fileName file name
   * @param documentType document type
   * @param sizeBytes size
   * @param uploadedBy user
   * @param uploadedAt time
   */
  public record DocumentView(
      Long id,
      String fileName,
      String documentType,
      long sizeBytes,
      String uploadedBy,
      Instant uploadedAt) {}

  /**
   * The cases of a queue.
   *
   * @param companyId company
   * @param queue queue
   * @param from first creation day, may be null
   * @param to last creation day, may be null
   * @return rows, newest first
   */
  public List<CaseRow> queue(Long companyId, Queue queue, LocalDate from, LocalDate to) {
    String me = currentUser.username();
    Set<CaseStatus> open = CaseStatus.open();
    Set<CaseStatus> closed = EnumSet.of(CaseStatus.CLOSED_BY_FULFILMENT, CaseStatus.CLOSED_BY_CSR);
    List<String> units = restrictedUnits().orElse(null);
    Filter f =
        switch (queue) {
          case MINE -> new Filter(companyId, open, me, units, from, to, false);
          case RETURNED ->
              new Filter(
                  companyId, EnumSet.of(CaseStatus.RETURNED_TO_AGENT), me, units, from, to, false);
          case CSR_ACTION ->
              new Filter(
                  companyId,
                  EnumSet.of(CaseStatus.CSR_ACTION_REQUIRED),
                  me,
                  units,
                  from,
                  to,
                  false);
          case TEAM -> {
            requireSupervisor();
            yield new Filter(companyId, open, null, units, from, to, false);
          }
          case UNIT ->
              new Filter(
                  companyId,
                  EnumSet.of(CaseStatus.WITH_FULFILMENT),
                  null,
                  setup.unitsOf(me),
                  from,
                  to,
                  false);
          case OPEN -> new Filter(companyId, open, null, units, from, to, false);
          case CLOSED -> new Filter(companyId, closed, null, units, from, to, false);
          case ALL ->
              new Filter(companyId, EnumSet.allOf(CaseStatus.class), null, units, from, to, false);
        };
    return views.rows(companyId, cases.list(f, MAX_ROWS));
  }

  /**
   * The cases of a client (Cases tab of the Servicing View).
   *
   * @param companyId company
   * @param clientId client
   * @return rows
   */
  public List<CaseRow> ofClient(Long companyId, Long clientId) {
    return views.rows(companyId, cases.ofClient(companyId, clientId));
  }

  /**
   * The case record.
   *
   * @param companyId company of the request
   * @param id case
   * @return record
   */
  public CaseRecord record(Long companyId, Long id) {
    CsfCase c = inCompany(companyId, id);
    List<Linked> linked = new ArrayList<>();
    if (c.parentCaseId() != null) {
      cases
          .find(c.parentCaseId())
          .ifPresent(
              p ->
                  linked.add(
                      new Linked(
                          p.id(), p.caseNo(), "ORIGINAL", p.status().label(), c.followUpReason())));
    }
    cases
        .followUps(c.id())
        .forEach(
            f ->
                linked.add(
                    new Linked(
                        f.id(), f.caseNo(), "FOLLOW_UP", f.status().label(), f.followUpReason())));
    return views.record(c, linked, service.actionsOn(c));
  }

  /**
   * The notes of a case, oldest first.
   *
   * @param companyId company
   * @param id case
   * @return notes
   */
  public List<NoteView> notes(Long companyId, Long id) {
    CsfCase c = inCompany(companyId, id);
    return cases.notes(c.id()).stream()
        .map((Note n) -> new NoteView(n.note(), n.author(), n.createdAt()))
        .toList();
  }

  /**
   * The history of a case, newest first: every action with the values before and after.
   *
   * @param companyId company
   * @param id case
   * @return entries
   */
  public List<HistoryView> history(Long companyId, Long id) {
    CsfCase c = inCompany(companyId, id);
    return audit.history(CsfCaseCodes.ENTITY, List.of(c.caseNo())).stream()
        .map(
            a ->
                new HistoryView(
                    a.getOccurredAt(),
                    a.getUsername(),
                    a.getAction().name(),
                    a.getSummary(),
                    a.getOldValue(),
                    a.getNewValue()))
        .toList();
  }

  /**
   * The documents of a case.
   *
   * @param companyId company
   * @param id case
   * @return documents
   */
  public List<DocumentView> documents(Long companyId, Long id) {
    CsfCase c = inCompany(companyId, id);
    return documents.list(target(c)).stream().map(CsfCaseQueryService::document).toList();
  }

  /**
   * The case, in the company of the request and readable by the user.
   *
   * @param companyId company
   * @param id case
   * @return case
   */
  public CsfCase inCompany(Long companyId, Long id) {
    CsfCase c = service.require(id);
    if (!c.companyId().equals(companyId)) {
      throw new ResourceNotFoundException("Case", id);
    }
    return c;
  }

  /**
   * The attachment target of a case.
   *
   * @param c case
   * @return target
   */
  public static AttachmentTarget target(CsfCase c) {
    return new AttachmentTarget(CsfCaseCodes.ENTITY, String.valueOf(c.id()));
  }

  /**
   * A document of a case, checked to belong to it.
   *
   * @param c case
   * @param attachmentId attachment
   * @return attachment
   */
  public Attachment requireDocument(CsfCase c, Long attachmentId) {
    return documents.list(target(c)).stream()
        .filter(a -> a.getId().equals(attachmentId))
        .findFirst()
        .orElseThrow(
            () -> new BusinessRuleException("CSF_CASE_DOCUMENT_UNKNOWN", "Document not found"));
  }

  private static DocumentView document(Attachment a) {
    return new DocumentView(
        a.getId(),
        a.getFileName(),
        a.getDocumentType(),
        a.getSizeBytes(),
        a.getCreatedBy(),
        a.getCreatedAt());
  }

  private Optional<List<String>> restrictedUnits() {
    if (currentUser.hasAuthority(Permission.CSF_VIEW.name())) {
      return Optional.empty();
    }
    return Optional.of(setup.unitsOf(currentUser.username()));
  }

  private void requireSupervisor() {
    if (!currentUser.hasAuthority(Permission.CSF_CASE_SUPERVISE.name())) {
      throw new AccessDeniedException("You are not permitted to perform this action");
    }
  }
}
