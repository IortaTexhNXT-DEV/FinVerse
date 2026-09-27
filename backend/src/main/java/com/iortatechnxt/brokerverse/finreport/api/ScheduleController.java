package com.iortatechnxt.brokerverse.finreport.api;

import com.iortatechnxt.brokerverse.finreport.api.dto.ScheduleDtos.CommentBody;
import com.iortatechnxt.brokerverse.finreport.api.dto.ScheduleDtos.CommentResponse;
import com.iortatechnxt.brokerverse.finreport.api.dto.ScheduleDtos.ScheduleBody;
import com.iortatechnxt.brokerverse.finreport.api.dto.ScheduleDtos.ScheduleResponse;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment.CommentKey;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleDefinitionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account schedule definitions and commentary (FRBS 3.2.0, Appendix A II-IV). A schedule is run and
 * exported as report {@code GL-SCHEDULE} with the parameter {@code schedule} through the Report
 * Centre endpoints.
 */
@RestController
@RequestMapping("/api/v1/finreport/schedules")
public class ScheduleController {

  private static final String VIEW =
      "hasAnyAuthority('FRBS_REPORT_VIEW', 'REPORT_FINANCIAL', 'MASTER_MAINTAIN')";
  private static final String MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";
  private static final String COMMENT = "hasAuthority('FRBS_REPORT_EXPORT')";

  private final ScheduleDefinitionService service;

  /**
   * Creates the controller.
   *
   * @param service schedule definitions
   */
  public ScheduleController(ScheduleDefinitionService service) {
    this.service = service;
  }

  /**
   * The schedule definitions.
   *
   * @param activeOnly only those offered
   * @return definitions in code order
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<ScheduleResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
    return service.list(activeOnly).stream().map(ScheduleResponse::from).toList();
  }

  /**
   * One definition.
   *
   * @param code code
   * @return definition
   */
  @GetMapping("/{code}")
  @PreAuthorize(VIEW)
  public ScheduleResponse get(@PathVariable String code) {
    return ScheduleResponse.from(service.get(code));
  }

  /**
   * Adds a schedule (FRBS configuration once BDOI confirms a layout, AQ05).
   *
   * @param body definition
   * @return definition
   */
  @PostMapping
  @PreAuthorize(MAINTAIN)
  public ScheduleResponse create(@Valid @RequestBody ScheduleBody body) {
    return ScheduleResponse.from(service.create(body.code(), body.values()));
  }

  /**
   * Changes a schedule.
   *
   * @param code code
   * @param body definition
   * @return definition
   */
  @PutMapping("/{code}")
  @PreAuthorize(MAINTAIN)
  public ScheduleResponse update(@PathVariable String code, @Valid @RequestBody ScheduleBody body) {
    return ScheduleResponse.from(service.update(code, body.values()));
  }

  /**
   * The commentary of a month (Appendix A II-45).
   *
   * @param code schedule
   * @param companyId company
   * @param period month {@code yyyy-MM}
   * @return comments
   */
  @GetMapping("/{code}/comments")
  @PreAuthorize(VIEW)
  public List<CommentResponse> comments(
      @PathVariable String code, @RequestParam Long companyId, @RequestParam String period) {
    return service.comments(companyId, code, period).stream().map(CommentResponse::from).toList();
  }

  /**
   * Keeps or removes the comment of a row for a month.
   *
   * @param code schedule
   * @param body company, month, row and text
   * @return the comment, or 204 when removed
   */
  @PutMapping("/{code}/comments")
  @PreAuthorize(COMMENT)
  public ResponseEntity<CommentResponse> comment(
      @PathVariable String code, @Valid @RequestBody CommentBody body) {
    StatementComment saved =
        service.comment(
            new CommentKey(body.companyId(), code, body.period(), body.rowKey()), body.text());
    return saved == null
        ? ResponseEntity.noContent().build()
        : ResponseEntity.ok(CommentResponse.from(saved));
  }
}
