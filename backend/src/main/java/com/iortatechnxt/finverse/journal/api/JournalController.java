package com.iortatechnxt.finverse.journal.api;

import com.iortatechnxt.finverse.common.api.PageResponse;
import com.iortatechnxt.finverse.common.api.ReasonRequest;
import com.iortatechnxt.finverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.finverse.journal.api.dto.JournalResponse;
import com.iortatechnxt.finverse.journal.api.dto.JournalSearchParams;
import com.iortatechnxt.finverse.journal.api.dto.ReverseJournalRequest;
import com.iortatechnxt.finverse.journal.service.JournalAuthorizationService;
import com.iortatechnxt.finverse.journal.service.JournalEntryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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

  /**
   * Creates the controller.
   *
   * @param entries maker services
   * @param authorization checker services
   */
  public JournalController(JournalEntryService entries, JournalAuthorizationService authorization) {
    this.entries = entries;
    this.authorization = authorization;
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
