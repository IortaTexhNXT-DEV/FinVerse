package com.iortatechnxt.brokerverse.renewal.insurer.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatch;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerBatchService;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerBatchService.Line;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerExtract;
import com.iortatechnxt.brokerverse.renewal.insurer.service.RenewalInsurerResponseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

/** Insurer batches and responses (FR-RN-070, 071). */
@RestController
@RequestMapping("/api/v1/renewal")
public class RenewalInsurerController {

  private static final String INSURER = "hasAuthority('RNW_INSURER')";
  private static final String VIEW = "hasAuthority('RNW_VIEW')";

  private final InsurerBatchService batches;
  private final RenewalInsurerResponseService responses;

  /**
   * Creates the controller.
   *
   * @param batches batches
   * @param responses responses
   */
  public RenewalInsurerController(
      InsurerBatchService batches, RenewalInsurerResponseService responses) {
    this.batches = batches;
    this.responses = responses;
  }

  /**
   * The batches of a company.
   *
   * @param companyId company
   * @return batches, newest first
   */
  @GetMapping("/insurer-batches")
  @PreAuthorize(VIEW)
  public List<BatchView> list(@RequestParam Long companyId) {
    return batches.list(companyId).stream().map(BatchView::of).toList();
  }

  /**
   * A batch with its lines.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return batch
   */
  @GetMapping("/insurer-batches/{batchNo}")
  @PreAuthorize(VIEW)
  public BatchDetail get(@RequestParam Long companyId, @PathVariable String batchNo) {
    return new BatchDetail(
        BatchView.of(batches.get(companyId, batchNo)),
        InsurerExtract.HEADERS,
        batches.lines(companyId, batchNo));
  }

  /**
   * Builds a batch.
   *
   * @param request insurer and range
   * @return batch
   */
  @PostMapping("/insurer-batches")
  @PreAuthorize(INSURER)
  public BatchView create(@Valid @RequestBody BatchRequest request) {
    return BatchView.of(
        batches.create(
            request.companyId(), request.insurerCode(), request.expiryFrom(), request.expiryTo()));
  }

  /**
   * The extract of a batch.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return spreadsheet
   */
  @GetMapping("/insurer-batches/{batchNo}/file.xlsx")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> file(@RequestParam Long companyId, @PathVariable String batchNo) {
    MessageFile f = batches.file(companyId, batchNo);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(f.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(f.fileName()))
        .body(f.content());
  }

  /**
   * Sends a batch to the insurer.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return batch
   */
  @PostMapping("/insurer-batches/{batchNo}/send")
  @PreAuthorize(INSURER)
  public BatchView send(@RequestParam Long companyId, @PathVariable String batchNo) {
    return BatchView.of(batches.send(companyId, batchNo));
  }

  /**
   * The insurer responses of a renewal.
   *
   * @param companyId company
   * @param ref renewal
   * @return responses, newest first
   */
  @GetMapping("/candidates/{ref}/insurer-responses")
  @PreAuthorize(VIEW)
  public List<ResponseView> responses(@RequestParam Long companyId, @PathVariable String ref) {
    return responses.of(companyId, ref).stream().map(ResponseView::of).toList();
  }

  /**
   * Records a response on the Insurer tab.
   *
   * @param companyId company
   * @param ref renewal
   * @param request response
   * @return response
   */
  @PostMapping("/candidates/{ref}/insurer-responses")
  @PreAuthorize(INSURER)
  public ResponseView record(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @Valid @RequestBody ResponseRequest request) {
    return ResponseView.of(
        responses.manual(
            companyId,
            ref,
            new InsurerResponse.Content(
                request.response(),
                request.insurerRef(),
                request.revisedPremium(),
                request.revisedSumInsured(),
                request.revisedRate(),
                request.terms(),
                request.receivedOn(),
                request.remarks()),
            request.remarket()));
  }

  /**
   * A new batch.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param expiryFrom first expiry
   * @param expiryTo last expiry
   */
  public record BatchRequest(
      @NotNull Long companyId,
      @NotBlank String insurerCode,
      @NotNull LocalDate expiryFrom,
      @NotNull LocalDate expiryTo) {}

  /**
   * A manual response.
   *
   * @param response response
   * @param insurerRef insurer reference
   * @param revisedPremium revised premium
   * @param revisedSumInsured revised sum insured
   * @param revisedRate revised rate
   * @param terms revised terms
   * @param receivedOn date received
   * @param remarks remarks
   * @param remarket for a Reject, return to Marketing
   */
  public record ResponseRequest(
      @NotNull com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode response,
      String insurerRef,
      BigDecimal revisedPremium,
      BigDecimal revisedSumInsured,
      BigDecimal revisedRate,
      String terms,
      LocalDate receivedOn,
      String remarks,
      boolean remarket) {}

  /**
   * A batch.
   *
   * @param batchNo number
   * @param insurerCode insurer
   * @param expiryFrom first expiry
   * @param expiryTo last expiry
   * @param status status
   * @param lineCount accounts
   * @param recipients mailbox
   * @param sentAt send time
   * @param sentBy sender
   * @param replyDue reply date
   * @param attachmentId stored extract
   */
  public record BatchView(
      String batchNo,
      String insurerCode,
      LocalDate expiryFrom,
      LocalDate expiryTo,
      String status,
      int lineCount,
      String recipients,
      Instant sentAt,
      String sentBy,
      LocalDate replyDue,
      Long attachmentId) {

    static BatchView of(InsurerBatch b) {
      return new BatchView(
          b.getBatchNo(),
          b.getInsurerCode(),
          b.getExpiryFrom(),
          b.getExpiryTo(),
          b.getStatus().name(),
          b.getLineCount(),
          b.getRecipients(),
          b.getSentAt(),
          b.getSentBy(),
          b.getReplyDue(),
          b.getAttachmentId());
    }
  }

  /**
   * A batch with its lines.
   *
   * @param batch batch
   * @param headers the 28 column headers
   * @param lines lines
   */
  public record BatchDetail(BatchView batch, List<String> headers, List<Line> lines) {}

  /**
   * A response.
   *
   * @param response response
   * @param insurerRef insurer reference
   * @param revisedPremium revised premium
   * @param revisedSumInsured revised sum insured
   * @param revisedRate revised rate
   * @param terms terms
   * @param receivedOn received
   * @param source UPLOAD or MANUAL
   * @param match match outcome
   * @param latestValid drives the renewal
   * @param late after the reply date
   * @param jobNo upload job
   * @param remarks remarks
   * @param by user
   * @param at time
   */
  public record ResponseView(
      String response,
      String insurerRef,
      BigDecimal revisedPremium,
      BigDecimal revisedSumInsured,
      BigDecimal revisedRate,
      String terms,
      LocalDate receivedOn,
      String source,
      String match,
      boolean latestValid,
      boolean late,
      String jobNo,
      String remarks,
      String by,
      Instant at) {

    static ResponseView of(InsurerResponse r) {
      return new ResponseView(
          r.getResponse().name(),
          r.getInsurerRef(),
          r.getRevisedPremium(),
          r.getRevisedSumInsured(),
          r.getRevisedRate(),
          r.getTerms(),
          r.getReceivedOn(),
          r.getSource(),
          r.getMatchOutcome().name(),
          r.isLatestValid(),
          r.isLate(),
          r.getJobNo(),
          r.getRemarks(),
          r.getCreatedBy(),
          r.getCreatedAt());
    }
  }
}
