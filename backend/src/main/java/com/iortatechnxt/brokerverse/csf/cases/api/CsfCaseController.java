package com.iortatechnxt.brokerverse.csf.cases.api;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.security.OwnedRecord;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseDocumentService;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseQueryService;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseQueryService.DocumentView;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseQueryService.HistoryView;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseQueryService.NoteView;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseQueryService.Queue;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseService;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseSetupService;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics.Criteria;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics.Dashboard;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.CaseRecord;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.CaseRow;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cases of the Customer Servicing Facility (FR-CSF-050 to 060): the work queues, the case record
 * with its notes, documents and history, the actions of the agent ({@code CSF_CASE_CREATE}), the
 * supervisor ({@code CSF_CASE_SUPERVISE}) and the fulfilment unit ({@code CSF_CASE_FULFIL}), and
 * the Case Dashboard ({@code CSF_CASE_REPORT}).
 */
@OwnedRecord(value = Client.class, variable = "clientId")
@OwnedRecord(type = "CsfCase")
@OwnedRecord(value = Attachment.class, variable = "attachmentId")
@RestController
@RequestMapping("/api/v1/csf/cases")
public class CsfCaseController {

  private static final String READ = "hasAnyAuthority('CSF_VIEW', 'CSF_CASE_FULFIL')";
  private static final String FULFIL = "hasAuthority('CSF_CASE_FULFIL')";
  private static final String CREATE = "hasAuthority('CSF_CASE_CREATE')";
  private static final String SUPERVISE = "hasAuthority('CSF_CASE_SUPERVISE')";

  private final CsfCaseService cases;
  private final CsfCaseQueryService queries;
  private final CsfCaseDocumentService documents;
  private final CsfCaseStatistics statistics;
  private final CsfCaseSetupService setup;
  private final AttachmentService attachments;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param cases actions
   * @param queries reads
   * @param documents documents
   * @param statistics dashboard
   * @param setup routing proposal
   * @param attachments upload size limit
   * @param downloads file answers
   */
  public CsfCaseController(
      CsfCaseService cases,
      CsfCaseQueryService queries,
      CsfCaseDocumentService documents,
      CsfCaseStatistics statistics,
      CsfCaseSetupService setup,
      AttachmentService attachments,
      FileDownloads downloads) {
    this.cases = cases;
    this.queries = queries;
    this.documents = documents;
    this.statistics = statistics;
    this.setup = setup;
    this.attachments = attachments;
    this.downloads = downloads;
  }

  /**
   * A fulfilment unit proposed by the routing table.
   *
   * @param fulfilmentUnit unit, null when no row matches
   */
  public record Proposal(String fulfilmentUnit) {}

  /**
   * The cases of a work queue.
   *
   * @param companyId company
   * @param queue queue
   * @param from first creation day
   * @param to last creation day
   * @return rows
   */
  @GetMapping
  @PreAuthorize(READ)
  public List<CaseRow> queue(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "OPEN") Queue queue,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return queries.queue(companyId, queue, from, to);
  }

  /**
   * The cases of a client (Cases tab of the Servicing View).
   *
   * @param companyId company
   * @param clientId client
   * @return rows
   */
  @GetMapping("/clients/{clientId}")
  @PreAuthorize("hasAuthority('CSF_VIEW')")
  public List<CaseRow> ofClient(@RequestParam Long companyId, @PathVariable Long clientId) {
    return queries.ofClient(companyId, clientId);
  }

  /**
   * The fulfilment unit the routing table proposes.
   *
   * @param companyId company
   * @param caseType case type
   * @param marketSegment market segment
   * @param nature nature of transaction
   * @return proposal
   */
  @GetMapping("/proposal")
  @PreAuthorize(CREATE)
  public Proposal propose(
      @RequestParam Long companyId,
      @RequestParam(required = false) String caseType,
      @RequestParam(required = false) String marketSegment,
      @RequestParam(required = false) String nature) {
    return new Proposal(setup.propose(caseType, marketSegment, nature).orElse(null));
  }

  /**
   * Logs a case: Submit to Fulfilment Unit, or Resolve and Close.
   *
   * @param companyId company
   * @param request case
   * @return the case record
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CREATE)
  public CaseRecord log(
      @RequestParam Long companyId, @Valid @RequestBody CaseRequests.LogCase request) {
    CsfCase c = cases.log(companyId, request.toRequest());
    return queries.record(companyId, c.id());
  }

  /**
   * The case record.
   *
   * @param companyId company
   * @param id case
   * @return record
   */
  @GetMapping("/{id}")
  @PreAuthorize(READ)
  public CaseRecord record(@RequestParam Long companyId, @PathVariable Long id) {
    return queries.record(companyId, id);
  }

  /**
   * The notes of a case.
   *
   * @param companyId company
   * @param id case
   * @return notes, oldest first
   */
  @GetMapping("/{id}/notes")
  @PreAuthorize(READ)
  public List<NoteView> notes(@RequestParam Long companyId, @PathVariable Long id) {
    return queries.notes(companyId, id);
  }

  /**
   * Adds a note.
   *
   * @param companyId company
   * @param id case
   * @param request note
   * @return notes
   */
  @PostMapping("/{id}/notes")
  @PreAuthorize(READ)
  public List<NoteView> addNote(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Text request) {
    queries.inCompany(companyId, id);
    cases.addNote(id, request.text());
    return queries.notes(companyId, id);
  }

  /**
   * The history of a case.
   *
   * @param companyId company
   * @param id case
   * @return entries, newest first
   */
  @GetMapping("/{id}/history")
  @PreAuthorize(READ)
  public List<HistoryView> history(@RequestParam Long companyId, @PathVariable Long id) {
    return queries.history(companyId, id);
  }

  /**
   * The documents of a case.
   *
   * @param companyId company
   * @param id case
   * @return documents
   */
  @GetMapping("/{id}/documents")
  @PreAuthorize(READ)
  public List<DocumentView> documents(@RequestParam Long companyId, @PathVariable Long id) {
    return queries.documents(companyId, id);
  }

  /**
   * Uploads a document to a case.
   *
   * @param companyId company
   * @param id case
   * @param documentType document type
   * @param file file
   * @return the document
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(READ)
  public DocumentView upload(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @RequestParam String documentType,
      @RequestParam MultipartFile file)
      throws IOException {
    attachments.requireWithinLimit(file.getSize());
    return documents.upload(
        companyId, id, documentType, new UploadedFile(file.getOriginalFilename(), file.getBytes()));
  }

  /**
   * Downloads a document of a case (logged).
   *
   * @param companyId company
   * @param id case
   * @param attachmentId document
   * @param request HTTP request
   * @return file
   */
  @GetMapping("/{id}/documents/{attachmentId}/content")
  @PreAuthorize(READ)
  public ResponseEntity<byte[]> download(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @PathVariable Long attachmentId,
      HttpServletRequest request) {
    return downloads.respond(documents.download(companyId, id, attachmentId), request);
  }

  /**
   * Resolves and closes a case (fulfilment unit).
   *
   * @param companyId company
   * @param id case
   * @param request resolution and closure reason
   * @return record
   */
  @PostMapping("/{id}/resolve")
  @PreAuthorize(FULFIL)
  public CaseRecord resolve(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Close request) {
    return act(
        companyId, id, () -> cases.resolve(id, request.resolution(), request.closureReason()));
  }

  /**
   * Returns a case to the agent (fulfilment unit).
   *
   * @param companyId company
   * @param id case
   * @param request return reason and comment
   * @return record
   */
  @PostMapping("/{id}/return")
  @PreAuthorize(FULFIL)
  public CaseRecord returnToAgent(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Reason request) {
    return act(companyId, id, () -> cases.returnToAgent(id, request.reason(), request.comment()));
  }

  /**
   * Asks the agent to act with the client (fulfilment unit).
   *
   * @param companyId company
   * @param id case
   * @param request instruction
   * @return record
   */
  @PostMapping("/{id}/csr-action")
  @PreAuthorize(FULFIL)
  public CaseRecord requestCsrAction(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Text request) {
    return act(companyId, id, () -> cases.requestCsrAction(id, request.text()));
  }

  /**
   * Re-endorses a case to the fulfilment unit (agent).
   *
   * @param companyId company
   * @param id case
   * @param request comment
   * @return record
   */
  @PostMapping("/{id}/reendorse")
  @PreAuthorize(CREATE)
  public CaseRecord reendorse(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Text request) {
    return act(companyId, id, () -> cases.reendorse(id, request.text()));
  }

  /**
   * Closes a case as Closed by CSR (agent or supervisor).
   *
   * @param companyId company
   * @param id case
   * @param request resolution and closure reason
   * @return record
   */
  @PostMapping("/{id}/close")
  @PreAuthorize("hasAnyAuthority('CSF_CASE_CREATE', 'CSF_CASE_SUPERVISE')")
  public CaseRecord close(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Close request) {
    return act(
        companyId, id, () -> cases.closeByCsr(id, request.resolution(), request.closureReason()));
  }

  /**
   * Reassigns a case (supervisor).
   *
   * @param companyId company
   * @param id case
   * @param request new owner or unit
   * @return record
   */
  @PostMapping("/{id}/reassign")
  @PreAuthorize(SUPERVISE)
  public CaseRecord reassign(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Reassign request) {
    return act(
        companyId,
        id,
        () -> cases.reassign(id, request.toUser(), request.toUnit(), request.comment()));
  }

  /**
   * Escalates a case (supervisor).
   *
   * @param companyId company
   * @param id case
   * @param request escalation reason and comment
   * @return record
   */
  @PostMapping("/{id}/escalate")
  @PreAuthorize(SUPERVISE)
  public CaseRecord escalate(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Reason request) {
    return act(companyId, id, () -> cases.escalate(id, request.reason(), request.comment()));
  }

  /**
   * Changes the category of a case (supervisor).
   *
   * @param companyId company
   * @param id case
   * @param request case type, nature and reason
   * @return record
   */
  @PostMapping("/{id}/category")
  @PreAuthorize(SUPERVISE)
  public CaseRecord changeCategory(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.ChangeCategory request) {
    return act(
        companyId,
        id,
        () -> cases.changeCategory(id, request.caseType(), request.nature(), request.reason()));
  }

  /**
   * Changes the priority of a case (supervisor).
   *
   * @param companyId company
   * @param id case
   * @param request priority
   * @return record
   */
  @PostMapping("/{id}/priority")
  @PreAuthorize(SUPERVISE)
  public CaseRecord changePriority(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Text request) {
    return act(companyId, id, () -> cases.changePriority(id, request.text()));
  }

  /**
   * Creates a follow-up case from a closed case (agent).
   *
   * @param companyId company
   * @param id closed case
   * @param request reason
   * @return the new case
   */
  @PostMapping("/{id}/follow-up")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CREATE)
  public CaseRecord followUp(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @Valid @RequestBody CaseRequests.Text request) {
    queries.inCompany(companyId, id);
    CsfCase created = cases.followUp(id, request.text());
    return queries.record(companyId, created.id());
  }

  /**
   * The Case Dashboard.
   *
   * @param companyId company
   * @param filter period and filters
   * @return figures
   */
  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('CSF_CASE_REPORT')")
  public Dashboard dashboard(
      @RequestParam Long companyId, @Valid @ModelAttribute CaseRequests.DashboardFilter filter) {
    return statistics.dashboard(
        new Criteria(
            companyId,
            filter.from(),
            filter.to(),
            filter.caseType(),
            filter.priority(),
            filter.status(),
            filter.marketSegment(),
            filter.fulfilmentUnit()));
  }

  private CaseRecord act(Long companyId, Long id, Supplier<CsfCase> action) {
    queries.inCompany(companyId, id);
    action.get();
    return queries.record(companyId, id);
  }
}
