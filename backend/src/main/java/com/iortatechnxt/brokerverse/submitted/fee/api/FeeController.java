package com.iortatechnxt.brokerverse.submitted.fee.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNoTouchBatch;
import com.iortatechnxt.brokerverse.submitted.fee.api.dto.FeeDtos.AmbiguousView;
import com.iortatechnxt.brokerverse.submitted.fee.api.dto.FeeDtos.BatchView;
import com.iortatechnxt.brokerverse.submitted.fee.api.dto.FeeDtos.CancelRequest;
import com.iortatechnxt.brokerverse.submitted.fee.api.dto.FeeDtos.ExportRequest;
import com.iortatechnxt.brokerverse.submitted.fee.api.dto.FeeDtos.FeeView;
import com.iortatechnxt.brokerverse.submitted.fee.api.dto.FeeDtos.LineView;
import com.iortatechnxt.brokerverse.submitted.fee.api.dto.FeeDtos.TagRequest;
import com.iortatechnxt.brokerverse.submitted.fee.api.dto.FeeDtos.TaggingView;
import com.iortatechnxt.brokerverse.submitted.fee.service.HandlingFeeService;
import com.iortatechnxt.brokerverse.submitted.fee.service.NoTouchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handling Fees and No Touch Billing (FR-SP-070 to 075): the billed fees and their tagging to the
 * unapplied payments, the payments matching several fees, the tagging by hand, and the monthly No
 * Touch batches of an insurer (export, return, billing).
 */
@RestController
@RequestMapping("/api/v1/submitted")
@PreAuthorize("hasAuthority('SBM_HANDLING_FEE')")
public class FeeController {

  private final HandlingFeeService fees;
  private final NoTouchService noTouch;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param fees handling fees
   * @param noTouch No Touch billing
   * @param downloads file answers
   */
  public FeeController(HandlingFeeService fees, NoTouchService noTouch, FileDownloads downloads) {
    this.fees = fees;
    this.noTouch = noTouch;
    this.downloads = downloads;
  }

  /**
   * Fees of a company by status.
   *
   * @param companyId company
   * @param status statuses
   * @param pageable page
   * @return fees
   */
  @GetMapping("/handling-fees")
  public PageResponse<FeeView> list(
      @RequestParam Long companyId, @RequestParam List<String> status, Pageable pageable) {
    return PageResponse.of(fees.list(companyId, status, pageable), FeeView::from);
  }

  /**
   * A fee.
   *
   * @param id fee
   * @return fee
   */
  @GetMapping("/handling-fees/{id}")
  public FeeView get(@PathVariable Long id) {
    return FeeView.from(fees.get(id));
  }

  /**
   * The payments matching several fees.
   *
   * @param companyId company
   * @return payments with their candidates
   */
  @GetMapping("/handling-fees/ambiguous")
  public List<AmbiguousView> ambiguous(@RequestParam Long companyId) {
    return fees.ambiguous(companyId).stream()
        .map(a -> AmbiguousView.from(a.payment(), a.fees()))
        .toList();
  }

  /**
   * Runs the tagger of a company now.
   *
   * @param companyId company
   * @return what it did
   */
  @PostMapping("/handling-fees/tag")
  public TaggingView tag(@RequestParam Long companyId) {
    HandlingFeeService.Tagging t = fees.tag(companyId);
    return new TaggingView(t.tagged(), t.ambiguous());
  }

  /**
   * Tags a fee to a payment by hand.
   *
   * @param id fee
   * @param r payment
   * @return fee
   */
  @PostMapping("/handling-fees/{id}/tag")
  public FeeView tagByHand(@PathVariable Long id, @Valid @RequestBody TagRequest r) {
    return FeeView.from(fees.tagByHand(id, r.unappliedRef()));
  }

  /**
   * Cancels a billed fee.
   *
   * @param id fee
   * @param r reason
   * @return fee
   */
  @PostMapping("/handling-fees/{id}/cancel")
  public FeeView cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest r) {
    return FeeView.from(fees.cancel(id, r.reason()));
  }

  /**
   * The No Touch batches of a company.
   *
   * @param companyId company
   * @return batches, newest first
   */
  @GetMapping("/no-touch")
  public List<BatchView> batches(@RequestParam Long companyId) {
    return noTouch.list(companyId).stream().map(BatchView::from).toList();
  }

  /**
   * Exports the No Touch records of an insurer and month.
   *
   * @param companyId company
   * @param r insurer and month
   * @return batch
   */
  @PostMapping("/no-touch")
  public BatchView export(@RequestParam Long companyId, @Valid @RequestBody ExportRequest r) {
    return BatchView.from(noTouch.export(companyId, r.insurerCode(), r.period()));
  }

  /**
   * The lines of a batch.
   *
   * @param id batch
   * @return lines
   */
  @GetMapping("/no-touch/{id}/lines")
  public List<LineView> lines(@PathVariable Long id) {
    return noTouch.lines(id).stream().map(LineView::from).toList();
  }

  /**
   * Bills a returned batch to the insurer.
   *
   * @param id batch
   * @return batch
   */
  @PostMapping("/no-touch/{id}/bill")
  public BatchView bill(@PathVariable Long id) {
    return BatchView.from(noTouch.bill(id));
  }

  /**
   * Downloads the export or the statement of a batch.
   *
   * @param id batch
   * @param part EXPORT or STATEMENT
   * @param request request
   * @return the file
   */
  @GetMapping("/no-touch/{id}/{part}")
  public ResponseEntity<byte[]> file(
      @PathVariable Long id, @PathVariable String part, HttpServletRequest request) {
    SbmNoTouchBatch b = noTouch.get(id);
    Long file = "statement".equalsIgnoreCase(part) ? b.getStatementFileId() : b.getExportedFileId();
    if (file == null) {
      throw new ResourceNotFoundException("No Touch file", id);
    }
    return downloads.respond(FileDownload.stored(file), request);
  }
}
