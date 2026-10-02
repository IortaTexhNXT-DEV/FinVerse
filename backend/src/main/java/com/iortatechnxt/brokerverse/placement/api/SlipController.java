package com.iortatechnxt.brokerverse.placement.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.AccountsRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.SlipEmailRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.SlipResponse;
import com.iortatechnxt.brokerverse.placement.domain.SlipStatus;
import com.iortatechnxt.brokerverse.placement.service.PlacementBatchService;
import com.iortatechnxt.brokerverse.placement.service.PlacementBatchService.ItemResult;
import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService;
import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService.SlipEmail;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placement slips (BRNB.069/071): list, generate, regenerate after a return, download the PDF and
 * XLSX, proposed e-mail, send and resend.
 */
@RestController
@RequestMapping("/api/v1/placement/slips")
public class SlipController {

  private static final int MAX_PAGE = 100;

  private final PlacementSlipService slips;
  private final PlacementBatchService batch;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param slips placement slips
   * @param batch multi-account actions
   * @param downloads file download answers
   */
  public SlipController(
      PlacementSlipService slips, PlacementBatchService batch, FileDownloads downloads) {
    this.downloads = downloads;
    this.slips = slips;
    this.batch = batch;
  }

  /**
   * Slips of a company, newest first.
   *
   * @param companyId company
   * @param status status filter
   * @param page page
   * @param size size
   * @return slips
   */
  @GetMapping
  @PreAuthorize(PlacementController.VIEW)
  public PageResponse<SlipResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) SlipStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        slips.search(
            companyId,
            status,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE))),
        SlipResponse::from);
  }

  /**
   * One slip.
   *
   * @param id slip
   * @return slip
   */
  @GetMapping("/{id}")
  @PreAuthorize(PlacementController.VIEW)
  public SlipResponse get(@PathVariable Long id) {
    return SlipResponse.from(slips.get(id));
  }

  /**
   * Generates the slips of accounts ready for placement, one per insurer branch.
   *
   * @param request company and accounts
   * @return new slips
   */
  @PostMapping("/generate")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PlacementController.MANAGE)
  public List<SlipResponse> generate(@Valid @RequestBody AccountsRequest request) {
    return slips.generate(request.companyId(), request.arns()).stream()
        .map(SlipResponse::from)
        .toList();
  }

  /**
   * Regenerates a slip after a return: next version, previous kept.
   *
   * @param id slip
   * @return new version
   */
  @PostMapping("/{id}/regenerate")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PlacementController.MANAGE)
  public SlipResponse regenerate(@PathVariable Long id) {
    return SlipResponse.from(slips.regenerate(id));
  }

  /**
   * The proposed e-mail to the insurer.
   *
   * @param id slip
   * @return draft
   */
  @GetMapping("/{id}/email-draft")
  @PreAuthorize(PlacementController.MANAGE)
  public SlipEmail draft(@PathVariable Long id) {
    return slips.draft(id);
  }

  /**
   * Sends or resends a slip to the insurer.
   *
   * @param id slip
   * @param request e-mail
   * @return slip
   */
  @PostMapping("/{id}/send")
  @PreAuthorize(PlacementController.MANAGE)
  public SlipResponse send(@PathVariable Long id, @Valid @RequestBody SlipEmailRequest request) {
    return SlipResponse.from(slips.send(id, request.toEmail()));
  }

  /**
   * Sends the generated slips of the selected accounts with the proposed e-mail.
   *
   * @param request company and accounts
   * @return outcome per account
   */
  @PostMapping("/send")
  @PreAuthorize(PlacementController.MANAGE)
  public List<ItemResult> sendForAccounts(@Valid @RequestBody AccountsRequest request) {
    return batch.sendSlips(request.arns());
  }

  /**
   * Downloads a slip file: a redirect to its presigned link, or the bytes of a file rendered before
   * ST1.
   *
   * @param id slip
   * @param format pdf or xlsx
   * @param request HTTP request (client address of the link audit)
   * @return redirect or file
   */
  @GetMapping("/{id}/files/{format}")
  @PreAuthorize(PlacementController.VIEW)
  public ResponseEntity<byte[]> file(
      @PathVariable Long id, @PathVariable String format, HttpServletRequest request) {
    return downloads.respond(slips.file(id, formatOf(format)), request);
  }

  private static String formatOf(String format) {
    return switch (format) {
      case "pdf", "PDF" -> PlacementSlipService.PDF;
      case "xlsx", "XLSX" -> PlacementSlipService.XLSX;
      default -> format;
    };
  }
}
