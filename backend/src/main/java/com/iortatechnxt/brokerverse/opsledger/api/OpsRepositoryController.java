package com.iortatechnxt.brokerverse.opsledger.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.opsledger.api.dto.QueueDtos.ExtractFileResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.QueueDtos.HandoffResponse;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * The in-system extract repository (shared-drive stand-in, OQ17) and the hand-offs of the default
 * port adapters (work to do by hand while a module is not active).
 */
@RestController
@RequestMapping("/api/v1/ops")
public class OpsRepositoryController {

  private final ExtractRepositoryService extracts;
  private final HandoffService handoffs;

  /**
   * Creates the controller.
   *
   * @param extracts extract repository
   * @param handoffs hand-offs
   */
  public OpsRepositoryController(ExtractRepositoryService extracts, HandoffService handoffs) {
    this.extracts = extracts;
    this.handoffs = handoffs;
  }

  /**
   * Files of the extract repository.
   *
   * @param companyId company
   * @param folder folder, optional
   * @return files, newest first
   */
  @GetMapping("/extracts")
  @PreAuthorize(OpsAccess.VIEW)
  public List<ExtractFileResponse> extracts(
      @RequestParam Long companyId, @RequestParam(required = false) String folder) {
    return extracts.list(companyId, folder).stream().map(ExtractFileResponse::from).toList();
  }

  /**
   * Downloads a file of the extract repository.
   *
   * @param id file
   * @return file
   */
  @GetMapping("/extracts/{id}/file")
  @PreAuthorize(OpsAccess.VIEW)
  public ResponseEntity<byte[]> extract(@PathVariable Long id) {
    ExtractFile file = extracts.download(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.getFileName()))
        .body(file.getContent());
  }

  /**
   * Hand-offs of a company.
   *
   * @param companyId company
   * @param status OPEN (default) or CLOSED
   * @param page page
   * @param size size
   * @return hand-offs, newest first
   */
  @GetMapping("/handoffs")
  @PreAuthorize(OpsAccess.VIEW)
  public PageResponse<HandoffResponse> handoffs(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "OPEN") OpsHandoff.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        handoffs.list(
            companyId,
            status,
            PageRequest.of(Math.max(page, 0), Math.min(size, OpsAccess.MAX_PAGE))),
        HandoffResponse::from);
  }

  /**
   * Closes a hand-off once done by hand.
   *
   * @param id hand-off
   * @param request what was done
   * @return hand-off
   */
  @PostMapping("/handoffs/{id}/close")
  @PreAuthorize(OpsAccess.HANDOFF_CLOSE)
  public HandoffResponse close(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return HandoffResponse.from(handoffs.close(id, request.reason()));
  }
}
