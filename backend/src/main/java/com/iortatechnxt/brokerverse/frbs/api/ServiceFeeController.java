package com.iortatechnxt.brokerverse.frbs.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.CommentBody;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.ComputeBody;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.ItemResponse;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.LineResponse;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.ReleaseBody;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.RunResponse;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeQueryService;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeRunService;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeTagService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service-fee runs (FRBS 2.10.0-2.10.2): the work list, one run with its lines and invoices,
 * compute, recompute, submit, approve (accrual and payout requests), send a returned line again,
 * and the RELEASED / LIQUIDATED tags with the unit's liquidation report. Return and cancel are the
 * generic actions of the workflow panel.
 */
@RestController
@RequestMapping("/api/v1/frbs/service-fee")
public class ServiceFeeController {

  private static final String RUN = "/runs/{id}";
  private static final String LINE = "/lines/{lineId}";

  private final ServiceFeeQueryService queries;
  private final ServiceFeeRunService runs;
  private final ServiceFeeTagService tags;

  /**
   * Creates the controller.
   *
   * @param queries reads
   * @param runs computation and approval
   * @param tags release and liquidation tags
   */
  public ServiceFeeController(
      ServiceFeeQueryService queries, ServiceFeeRunService runs, ServiceFeeTagService tags) {
    this.queries = queries;
    this.runs = runs;
    this.tags = tags;
  }

  /**
   * Searches runs (FRBS 2.10.0 "monitor the service fee").
   *
   * @param companyId company
   * @param stage stage
   * @param q run number contains
   * @param page page
   * @param size size
   * @return runs, newest first
   */
  @GetMapping("/runs")
  @PreAuthorize(FrbsAccess.VIEW)
  public PageResponse<RunResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) RunStage stage,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), FrbsAccess.MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(queries.search(companyId, stage, q, pageable), RunResponse::from);
  }

  /**
   * Runs per stage.
   *
   * @param companyId company
   * @return count per stage
   */
  @GetMapping("/runs/counts")
  @PreAuthorize(FrbsAccess.VIEW)
  public Map<RunStage, Long> counts(@RequestParam Long companyId) {
    return queries.counts(companyId);
  }

  /**
   * One run.
   *
   * @param id run
   * @return run
   */
  @GetMapping(RUN)
  @PreAuthorize(FrbsAccess.VIEW)
  public RunResponse get(@PathVariable Long id) {
    return RunResponse.from(queries.get(id));
  }

  /**
   * The lines of a run.
   *
   * @param id run
   * @return lines
   */
  @GetMapping(RUN + "/lines")
  @PreAuthorize(FrbsAccess.VIEW)
  public List<LineResponse> lines(@PathVariable Long id) {
    return queries.lines(id).stream().map(LineResponse::from).toList();
  }

  /**
   * The invoices of a run.
   *
   * @param id run
   * @return invoices
   */
  @GetMapping(RUN + "/invoices")
  @PreAuthorize(FrbsAccess.VIEW)
  public List<ItemResponse> invoices(@PathVariable Long id) {
    return queries.items(id).stream().map(ItemResponse::from).toList();
  }

  /**
   * Computes a run for the invoices fully paid in a period.
   *
   * @param companyId company
   * @param body period
   * @return run
   */
  @PostMapping("/runs")
  @PreAuthorize(FrbsAccess.MANAGE)
  public RunResponse compute(@RequestParam Long companyId, @Valid @RequestBody ComputeBody body) {
    return RunResponse.from(runs.compute(companyId, body.from(), body.to()));
  }

  /**
   * Computes a run again.
   *
   * @param id run
   * @return run
   */
  @PostMapping(RUN + "/recompute")
  @PreAuthorize(FrbsAccess.MANAGE)
  public RunResponse recompute(@PathVariable Long id) {
    return RunResponse.from(runs.recompute(id));
  }

  /**
   * Submits a run for approval.
   *
   * @param id run
   * @param body comment
   * @return run
   */
  @PostMapping(RUN + "/submit")
  @PreAuthorize(FrbsAccess.MANAGE)
  public RunResponse submit(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return RunResponse.from(runs.submit(id, body.comment()));
  }

  /**
   * Approves a run: accrual and payout requests.
   *
   * @param id run
   * @param body comment
   * @return run
   */
  @PostMapping(RUN + "/approve")
  @PreAuthorize(FrbsAccess.APPROVE)
  public RunResponse approve(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return RunResponse.from(runs.approve(id, body.comment()));
  }

  /**
   * Sends a returned line to Disbursement again.
   *
   * @param lineId line
   * @return line
   */
  @PostMapping(LINE + "/resend")
  @PreAuthorize(FrbsAccess.MANAGE)
  public LineResponse resend(@PathVariable Long lineId) {
    return LineResponse.from(runs.resend(lineId));
  }

  /**
   * Tags a line released (FRBS 2.10.2).
   *
   * @param lineId line
   * @param body release date
   * @return line
   */
  @PostMapping(LINE + "/release")
  @PreAuthorize(FrbsAccess.TAG)
  public LineResponse release(@PathVariable Long lineId, @Valid @RequestBody ReleaseBody body) {
    return LineResponse.from(tags.release(lineId, body.releasedOn()));
  }

  /**
   * Tags a line liquidated with the unit's liquidation report (FRBS 2.10.1-2.10.2).
   *
   * @param lineId line
   * @param liquidatedOn liquidation date
   * @param remarks remarks
   * @param file liquidation report
   * @return line
   * @throws IOException when the file cannot be read
   */
  @PostMapping(value = LINE + "/liquidate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(FrbsAccess.TAG)
  public LineResponse liquidate(
      @PathVariable Long lineId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate liquidatedOn,
      @RequestParam(required = false) String remarks,
      @RequestParam MultipartFile file)
      throws IOException {
    return LineResponse.from(
        tags.liquidate(lineId, liquidatedOn, remarks, file.getOriginalFilename(), file.getBytes()));
  }
}
