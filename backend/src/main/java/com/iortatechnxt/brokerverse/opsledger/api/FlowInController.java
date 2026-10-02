package com.iortatechnxt.brokerverse.opsledger.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.FlowInDtos.FeedConfigRequest;
import com.iortatechnxt.brokerverse.opsledger.api.dto.FlowInDtos.FeedResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.FlowInDtos.RecordResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.FlowInDtos.RunResponse;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInFeed;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interfaces with other systems (BRQID.004/005): feeds, run log with record outcomes, schedule and
 * activation, and the manual upload transport. Interface administrators only ({@code
 * FLOWIN_MANAGE}).
 */
@RestController
@RequestMapping("/api/v1/ops/flow-in")
@PreAuthorize(OpsAccess.FLOWIN)
public class FlowInController {

  private final FlowInService flowIn;

  /**
   * Creates the controller.
   *
   * @param flowIn flow-in service
   */
  public FlowInController(FlowInService flowIn) {
    this.flowIn = flowIn;
  }

  /**
   * Every feed.
   *
   * @return feeds by partner system
   */
  @GetMapping("/feeds")
  public List<FeedResponse> feeds() {
    return flowIn.feeds().stream().map(this::feed).toList();
  }

  /**
   * Changes a feed's schedule and activation.
   *
   * @param code feed
   * @param request schedule and activation
   * @return feed
   */
  @PutMapping("/feeds/{code}")
  public FeedResponse configure(
      @PathVariable String code, @Valid @RequestBody FeedConfigRequest request) {
    return feed(flowIn.configure(code, request.cron(), Boolean.TRUE.equals(request.active())));
  }

  /**
   * Uploads a file of a feed (manual transport).
   *
   * @param code feed
   * @param file file
   * @return the run
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/feeds/{code}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public RunResponse upload(@PathVariable String code, @RequestParam MultipartFile file)
      throws IOException {
    return RunResponse.from(
        flowIn.upload(code, new FlowInFile(file.getOriginalFilename(), file.getBytes())));
  }

  /**
   * Runs, newest first.
   *
   * @param feed feed, optional
   * @param page page
   * @param size size
   * @return runs
   */
  @GetMapping("/runs")
  public PageResponse<RunResponse> runs(
      @RequestParam(required = false) String feed,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        flowIn.runs(feed, PageRequest.of(Math.max(page, 0), Math.min(size, OpsAccess.MAX_PAGE))),
        RunResponse::from);
  }

  /**
   * Records of a run.
   *
   * @param id run
   * @param page page
   * @param size size
   * @return records
   */
  @GetMapping("/runs/{id}/records")
  public PageResponse<RecordResponse> records(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        flowIn.records(id, PageRequest.of(Math.max(page, 0), Math.min(size, OpsAccess.MAX_PAGE))),
        RecordResponse::from);
  }

  private FeedResponse feed(FlowInFeed f) {
    return FeedResponse.from(f, flowIn.hasHandler(f.getCode()));
  }
}
