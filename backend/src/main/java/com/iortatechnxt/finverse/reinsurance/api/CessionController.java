package com.iortatechnxt.finverse.reinsurance.api;

import com.iortatechnxt.finverse.reinsurance.api.dto.AllocationRunRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.CessionResponse;
import com.iortatechnxt.finverse.reinsurance.service.AllocationPreviewRow;
import com.iortatechnxt.finverse.reinsurance.service.AllocationRunResult;
import com.iortatechnxt.finverse.reinsurance.service.AllocationRunService;
import com.iortatechnxt.finverse.reinsurance.service.CessionService;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for cessions and the RI allocation run (preview and post). */
@RestController
@RequestMapping("/api/v1/reinsurance")
public class CessionController {

  private static final LocalDate OPEN_START = LocalDate.of(1900, 1, 1);
  private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

  private final CessionService cessions;
  private final AllocationRunService runs;

  /**
   * Creates the controller.
   *
   * @param cessions cession service
   * @param runs allocation run
   */
  public CessionController(CessionService cessions, AllocationRunService runs) {
    this.cessions = cessions;
    this.runs = runs;
  }

  /**
   * Planned allocation of the transactions of a period not yet ceded.
   *
   * @param companyId company
   * @param from first approval date
   * @param to last approval date
   * @return preview rows
   */
  @GetMapping("/allocation/preview")
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public List<AllocationPreviewRow> preview(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return runs.preview(companyId, from, to);
  }

  /**
   * Cedes the transactions of a period not yet ceded.
   *
   * @param request period
   * @return run result
   */
  @PostMapping("/allocation/runs")
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public AllocationRunResult run(@Valid @RequestBody AllocationRunRequest request) {
    return runs.post(request.companyId(), request.fromDate(), request.toDate(), JobTrigger.MANUAL);
  }

  /**
   * Lists cessions of a policy (by number) or of an RI accounting period.
   *
   * @param companyId company
   * @param policyNo policy number (takes precedence)
   * @param from first RI date
   * @param to last RI date
   * @return cessions
   */
  @GetMapping("/cessions")
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public List<CessionResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) String policyNo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    if (policyNo != null && !policyNo.isBlank()) {
      return cessions.ofPolicyNumber(companyId, policyNo.trim()).stream()
          .map(CessionResponse::from)
          .toList();
    }
    LocalDate end = to == null ? OPEN_END : to;
    LocalDate start = from == null ? OPEN_START : from;
    return cessions.list(companyId, start, end).stream().map(CessionResponse::from).toList();
  }

  /**
   * Gets a cession.
   *
   * @param id id
   * @return cession
   */
  @GetMapping("/cessions/{id}")
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public CessionResponse get(@PathVariable Long id) {
    return CessionResponse.from(cessions.get(id));
  }

  /**
   * Cedes the approved transactions of a policy not yet ceded (on demand).
   *
   * @param policyId policy
   * @return all cessions of the policy
   */
  @PostMapping("/cessions/policies/{policyId}")
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public List<CessionResponse> cedePolicy(@PathVariable Long policyId) {
    return cessions.cedePolicy(policyId).stream().map(CessionResponse::from).toList();
  }
}
