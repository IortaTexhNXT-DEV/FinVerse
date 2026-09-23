package com.iortatechnxt.finverse.closing.api;

import com.iortatechnxt.finverse.closing.api.dto.ChecklistResponse;
import com.iortatechnxt.finverse.closing.api.dto.YearEndCloseResponse;
import com.iortatechnxt.finverse.closing.api.dto.YearEndRequest;
import com.iortatechnxt.finverse.closing.service.ClosingBalanceQuery.PnlBalance;
import com.iortatechnxt.finverse.closing.service.ClosingChecklistService;
import com.iortatechnxt.finverse.closing.service.YearEndService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Period-end and year-end checklists and the year-end close. */
@RestController
@RequestMapping("/api/v1/closing")
public class ClosingController {

  private static final String CHECKLIST_VIEW = "hasAnyAuthority('PERIOD_END_RUN','YEAR_END_CLOSE')";

  private final ClosingChecklistService checklist;
  private final YearEndService yearEnd;

  /**
   * Creates the controller.
   *
   * @param checklist checklist service
   * @param yearEnd year-end service
   */
  public ClosingController(ClosingChecklistService checklist, YearEndService yearEnd) {
    this.checklist = checklist;
    this.yearEnd = yearEnd;
  }

  /**
   * Monthly close checklist.
   *
   * @param companyId company
   * @param periodId period
   * @return checklist
   */
  @GetMapping("/period-end/checklist")
  @PreAuthorize(CHECKLIST_VIEW)
  public ChecklistResponse periodEnd(@RequestParam Long companyId, @RequestParam Long periodId) {
    return ChecklistResponse.of(checklist.periodEnd(companyId, periodId));
  }

  /**
   * Year-end pre-close checklist.
   *
   * @param companyId company
   * @param fiscalYearId year
   * @return checklist
   */
  @GetMapping("/year-end/checklist")
  @PreAuthorize(CHECKLIST_VIEW)
  public ChecklistResponse yearEnd(@RequestParam Long companyId, @RequestParam Long fiscalYearId) {
    return ChecklistResponse.of(checklist.yearEnd(companyId, fiscalYearId));
  }

  /**
   * Preview of the closing entries.
   *
   * @param companyId company
   * @param fiscalYearId year
   * @return income and expense balances to close
   */
  @GetMapping("/year-end/preview")
  @PreAuthorize("hasAuthority('YEAR_END_CLOSE')")
  public List<PnlBalance> preview(@RequestParam Long companyId, @RequestParam Long fiscalYearId) {
    return yearEnd.preview(companyId, fiscalYearId);
  }

  /**
   * Close record of a fiscal year.
   *
   * @param fiscalYearId year
   * @return record, or 204 when the year was not closed by year-end processing
   */
  @GetMapping("/year-end/close")
  @PreAuthorize(CHECKLIST_VIEW)
  public ResponseEntity<YearEndCloseResponse> closeRecord(@RequestParam Long fiscalYearId) {
    return yearEnd
        .closeRecord(fiscalYearId)
        .map(YearEndCloseResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * Closes a fiscal year.
   *
   * @param request request
   * @return close record
   */
  @PostMapping("/year-end/close")
  @PreAuthorize("hasAuthority('YEAR_END_CLOSE')")
  public YearEndCloseResponse close(@Valid @RequestBody YearEndRequest request) {
    return YearEndCloseResponse.from(yearEnd.close(request.companyId(), request.fiscalYearId()));
  }
}
