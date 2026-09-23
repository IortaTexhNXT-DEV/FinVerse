package com.iortatechnxt.finverse.journal.api;

import com.iortatechnxt.finverse.journal.api.dto.RecurringTemplateRequest;
import com.iortatechnxt.finverse.journal.api.dto.RecurringTemplateResponse;
import com.iortatechnxt.finverse.journal.domain.RecurringJournalTemplate;
import com.iortatechnxt.finverse.journal.service.GenerationResult;
import com.iortatechnxt.finverse.journal.service.RecurringJournalService;
import com.iortatechnxt.finverse.journal.service.RecurringJournalService.OccurrenceView;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Recurring / accrual journal templates: maintenance, "run now" and generation history. */
@RestController
@RequestMapping("/api/v1/journals/recurring")
public class RecurringJournalController {

  private static final String VIEW = "hasAuthority('JOURNAL_VIEW')";
  private static final String CREATE = "hasAuthority('JOURNAL_CREATE')";

  private final RecurringJournalService service;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param service recurring journal service
   * @param clock clock
   */
  public RecurringJournalController(RecurringJournalService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  /**
   * Lists templates.
   *
   * @param companyId company
   * @return templates
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<RecurringTemplateResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(this::toResponse).toList();
  }

  /**
   * Gets a template.
   *
   * @param id id
   * @return template
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public RecurringTemplateResponse get(@PathVariable Long id) {
    return toResponse(service.get(id));
  }

  /**
   * Creates a template.
   *
   * @param request request
   * @return template
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CREATE)
  public RecurringTemplateResponse create(@Valid @RequestBody RecurringTemplateRequest request) {
    return toResponse(service.create(request));
  }

  /**
   * Updates a template.
   *
   * @param id id
   * @param request request
   * @return template
   */
  @PutMapping("/{id}")
  @PreAuthorize(CREATE)
  public RecurringTemplateResponse update(
      @PathVariable Long id, @Valid @RequestBody RecurringTemplateRequest request) {
    return toResponse(service.update(id, request));
  }

  /**
   * Activates a template.
   *
   * @param id id
   * @return template
   */
  @PostMapping("/{id}/activate")
  @PreAuthorize(CREATE)
  public RecurringTemplateResponse activate(@PathVariable Long id) {
    return toResponse(service.setActive(id, true));
  }

  /**
   * Deactivates a template.
   *
   * @param id id
   * @return template
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize(CREATE)
  public RecurringTemplateResponse deactivate(@PathVariable Long id) {
    return toResponse(service.setActive(id, false));
  }

  /**
   * Generates the due occurrences of a template now.
   *
   * @param id id
   * @param date run date (default today)
   * @return generation result
   */
  @PostMapping("/{id}/run")
  @PreAuthorize(CREATE)
  public GenerationResult run(
      @PathVariable Long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    return service.runTemplate(id, date != null ? date : LocalDate.now(clock));
  }

  /**
   * Generation history of a template.
   *
   * @param id id
   * @return occurrences
   */
  @GetMapping("/{id}/occurrences")
  @PreAuthorize(VIEW)
  public List<OccurrenceView> occurrences(@PathVariable Long id) {
    return service.history(id);
  }

  private RecurringTemplateResponse toResponse(RecurringJournalTemplate t) {
    return RecurringTemplateResponse.from(t);
  }
}
