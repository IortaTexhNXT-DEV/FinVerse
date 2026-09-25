package com.iortatechnxt.brokerverse.journal.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalAssignRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalIdsRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalResponse;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalSearchParams;
import com.iortatechnxt.brokerverse.journal.api.dto.ReverseJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.JournalAuthorizationService;
import com.iortatechnxt.brokerverse.journal.service.JournalBulkApprovals;
import com.iortatechnxt.brokerverse.journal.service.JournalEntryService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API for journal entry, authorization, reversal and inquiry. */
@RestController
@RequestMapping("/api/v1/journals")
public class JournalController {

  private static final int MAX_PAGE_SIZE = 200;
  private static final String CREATE = "hasAuthority('JOURNAL_CREATE')";

  private final JournalEntryService entries;
  private final JournalAuthorizationService authorization;
  private final JournalBulkApprovals bulkApprovals;
  private final UserDirectory users;

  /**
   * Creates the controller.
   *
   * @param entries maker services
   * @param authorization checker services
   * @param bulkApprovals bulk posting (FRBS 2.5.6)
   * @param users user facts (assignees)
   */
  public JournalController(
      JournalEntryService entries,
      JournalAuthorizationService authorization,
      JournalBulkApprovals bulkApprovals,
      UserDirectory users) {
    this.entries = entries;
    this.authorization = authorization;
    this.bulkApprovals = bulkApprovals;
    this.users = users;
  }

  /**
   * Users who may be assigned journals for posting (FRBS 2.5.1).
   *
   * @return user names
   */
  @GetMapping("/assignees")
  @PreAuthorize("hasAuthority('JOURNAL_ASSIGN')")
  public List<String> assignees() {
    return users.usersWithPermission("JOURNAL_AUTHORIZE");
  }

  /**
   * Assigns journals to the user who will post them, or clears the assignment (FRBS 2.5.1).
   *
   * @param request journals and assignee
   * @return assigned journals
   */
  @PostMapping("/assign")
  @PreAuthorize("hasAuthority('JOURNAL_ASSIGN')")
  public List<JournalResponse> assign(@Valid @RequestBody JournalAssignRequest request) {
    return entries.assign(request.ids(), request.assignee()).stream()
        .map(JournalResponse::summary)
        .toList();
  }

  /**
   * Posts several journals, each on its own (FRBS 2.5.6).
   *
   * @param request journals
   * @return outcome of each journal
   */
  @PostMapping("/bulk-approve")
  @PreAuthorize("hasAuthority('JOURNAL_AUTHORIZE')")
  public List<JournalBulkApprovals.Outcome> bulkApprove(
      @Valid @RequestBody JournalIdsRequest request) {
    return bulkApprovals.approveAll(request.ids());
  }

  /**
   * Non-blocking warnings of a journal, shown in the confirmation before submit and approve (FRBS
   * 2.5.5, 2.5.10, 2.8.3, 2.8.4).
   *
   * @param id journal
   * @return warnings
   */
  @GetMapping("/{id}/warnings")
  @PreAuthorize("hasAuthority('JOURNAL_VIEW')")
  public List<String> warnings(@PathVariable Long id) {
    return entries.warnings(id);
  }

  /**
   * Searches journals (inquiry / transaction checklist).
   *
   * @param params filters
   * @param page page index
   * @param size page size
   * @return page of journals
   */
  @GetMapping
  @PreAuthorize("hasAuthority('JOURNAL_VIEW')")
  public PageResponse<JournalResponse> search(
      @Valid @ModelAttribute JournalSearchParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    var pageable =
        PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(entries.search(params.toCriteria(), pageable), JournalResponse::summary);
  }

  /**
   * Gets a journal with lines.
   *
   * @param id id
   * @return journal
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('JOURNAL_VIEW')")
  public JournalResponse get(@PathVariable Long id) {
    return JournalResponse.withLines(entries.get(id));
  }

  /**
   * Creates a draft journal.
   *
   * @param request request
   * @return draft
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CREATE)
  public JournalResponse create(@Valid @RequestBody JournalRequest request) {
    return JournalResponse.withLines(entries.createDraft(request));
  }

  /**
   * Updates a draft or rejected journal.
   *
   * @param id id
   * @param request request
   * @return journal
   */
  @PutMapping("/{id}")
  @PreAuthorize(CREATE)
  public JournalResponse update(@PathVariable Long id, @Valid @RequestBody JournalRequest request) {
    return JournalResponse.withLines(entries.update(id, request));
  }

  /**
   * Submits a journal for authorization.
   *
   * @param id id
   * @return journal
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(CREATE)
  public JournalResponse submit(@PathVariable Long id) {
    return JournalResponse.withLines(entries.submit(id));
  }

  /**
   * Authorizes and posts a journal.
   *
   * @param id id
   * @return journal
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('JOURNAL_AUTHORIZE')")
  public JournalResponse approve(@PathVariable Long id) {
    return JournalResponse.withLines(authorization.approve(id));
  }

  /**
   * Rejects a journal.
   *
   * @param id id
   * @param request reason
   * @return journal
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('JOURNAL_AUTHORIZE')")
  public JournalResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return JournalResponse.withLines(authorization.reject(id, request.reason()));
  }

  /**
   * Cancels a journal.
   *
   * @param id id
   * @return journal
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(CREATE)
  public JournalResponse cancel(@PathVariable Long id) {
    return JournalResponse.withLines(entries.cancel(id));
  }

  /**
   * Raises a reversal of a posted journal.
   *
   * @param id id
   * @param request reversal request
   * @return reversal journal pending approval
   */
  @PostMapping("/{id}/reverse")
  @PreAuthorize("hasAuthority('JOURNAL_REVERSE')")
  public JournalResponse reverse(
      @PathVariable Long id, @Valid @RequestBody ReverseJournalRequest request) {
    return JournalResponse.withLines(
        authorization.reverse(id, request.reversalDate(), request.reason()));
  }

  /**
   * Copies a journal into a new draft.
   *
   * @param id id
   * @param valueDate value date for the copy
   * @return new draft
   */
  @PostMapping("/{id}/copy")
  @PreAuthorize(CREATE)
  public JournalResponse copy(
      @PathVariable Long id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valueDate) {
    return JournalResponse.withLines(entries.copy(id, valueDate));
  }
}
