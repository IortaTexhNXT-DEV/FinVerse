package com.iortatechnxt.brokerverse.remittance.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.security.OwnedRecord;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.party.service.ReinsuranceFunctions;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.AssignRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.BatchResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.BatchSummary;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.CommentRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.DeliveryResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.ExcludeRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.PreviewResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.QueuedResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.RestoreRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.ReturnRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.SendScheduleRequest;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.DocumentKind;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.service.BatchDocumentStore;
import com.iortatechnxt.brokerverse.remittance.service.BatchDocuments;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.remittance.service.ExtractDelivery;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceQueryService;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceQueryService.BatchSearch;
import com.iortatechnxt.brokerverse.remittance.service.ScheduleDispatch;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
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
 * Remittance batches and Process Remittance (RMTID.002/007-011/019/024/027/029/036, MKTID.001):
 * batch lists by stage, the batch page, exclusion and restore, preview, submit, approve, return,
 * re-assignment, the schedule and payment request documents and the schedule e-mail to the insurer.
 * Generic actions (hold, release, send back) run through the workflow panel.
 */
@OwnedRecord(RemittanceBatch.class)
@RestController
@RequestMapping("/api/v1/remittance/batches")
public class RemittanceBatchController {

  private final BatchService batches;
  private final RemittanceQueryService queries;
  private final BatchDocuments documents;
  private final ScheduleDispatch dispatch;
  private final FileDownloads downloads;
  private final BatchDocumentStore store;
  private final ExtractDelivery delivery;
  private final ReinsuranceFunctions reinsurance;

  /**
   * Creates the controller.
   *
   * @param batches batch actions
   * @param queries reads
   * @param documents schedule and payment request
   * @param dispatch schedule e-mail
   * @param downloads file download answers
   * @param store stored batch documents
   * @param delivery extract files and their copies to the shared folder
   * @param reinsurance batches of reinsurers hidden until Phase 2 (Q-RM-17)
   */
  public RemittanceBatchController(
      BatchService batches,
      RemittanceQueryService queries,
      BatchDocuments documents,
      ScheduleDispatch dispatch,
      FileDownloads downloads,
      BatchDocumentStore store,
      ExtractDelivery delivery,
      ReinsuranceFunctions reinsurance) {
    this.store = store;
    this.delivery = delivery;
    this.reinsurance = reinsurance;
    this.downloads = downloads;
    this.batches = batches;
    this.queries = queries;
    this.documents = documents;
    this.dispatch = dispatch;
  }

  /**
   * Batches, newest first (RMTID.024/027).
   *
   * @param companyId company
   * @param stage stages
   * @param insurer insurer
   * @param type remittance type
   * @param q batch or invoice number
   * @param page page
   * @param size size
   * @return batches
   */
  @GetMapping
  @PreAuthorize(RemittanceAccess.BATCH_READ)
  public PageResponse<BatchSummary> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<BatchStage> stage,
      @RequestParam(required = false) String insurer,
      @RequestParam(required = false) RemittanceType type,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.batches(
            new BatchSearch(companyId, stage, insurer, type, q, reinsurance.allowed()),
            RemittanceAccess.newestFirst(page, size)),
        BatchSummary::from);
  }

  /**
   * One batch with its accounts.
   *
   * @param id batch
   * @return batch
   */
  @GetMapping("/{id}")
  @PreAuthorize(RemittanceAccess.BATCH_READ)
  public BatchResponse get(@PathVariable Long id) {
    return response(visible(batches.get(id)));
  }

  /**
   * An extract file of the batch (FRS BRD-02 FRID.RMT.001.03): the Excel 97-2003 extract
   * (IF-RMT-01) or the pipe-delimited text extract (IF-RMT-02), named by the Remittance Settings.
   *
   * @param id batch
   * @param format excel or text
   * @return file
   */
  @GetMapping("/{id}/extract/{format}")
  @PreAuthorize(RemittanceAccess.BATCH_READ)
  public ResponseEntity<byte[]> extract(@PathVariable Long id, @PathVariable ExtractFormat format) {
    visible(batches.get(id));
    ExtractDelivery.RenderedFile file = delivery.file(id, format == ExtractFormat.EXCEL);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.name()).build().toString())
        .contentType(MediaType.parseMediaType(file.contentType()))
        .body(file.content());
  }

  /**
   * Copies of the extract files of the batch to the shared folder, newest first (IF-RMT-04).
   *
   * @param id batch
   * @return copies
   */
  @GetMapping("/{id}/deliveries")
  @PreAuthorize(RemittanceAccess.BATCH_READ)
  public List<DeliveryResponse> deliveries(@PathVariable Long id) {
    visible(batches.get(id));
    return delivery.history(id).stream().map(DeliveryResponse::from).toList();
  }

  /**
   * Copies the extract files of the batch to the shared folder again (after a failed copy).
   *
   * @param id batch
   * @return the copies made
   */
  @PostMapping("/{id}/deliveries")
  @PreAuthorize(RemittanceAccess.PROCESS)
  public List<DeliveryResponse> resend(@PathVariable Long id) {
    visible(batches.get(id));
    return delivery.resend(id).stream().map(DeliveryResponse::from).toList();
  }

  private RemittanceBatch visible(RemittanceBatch batch) {
    reinsurance.requireForParty(batch.getCompanyId(), batch.getInsurerCode());
    return batch;
  }

  /** Format of an extract file. */
  public enum ExtractFormat {
    /** Excel 97-2003 (IF-RMT-01). */
    EXCEL,
    /** Pipe-delimited text (IF-RMT-02). */
    TEXT
  }

  /**
   * What would be submitted (RMTID.002 preview).
   *
   * @param id batch
   * @return preview
   */
  @GetMapping("/{id}/preview")
  @PreAuthorize(RemittanceAccess.BATCH_READ)
  public PreviewResponse preview(@PathVariable Long id) {
    return PreviewResponse.from(batches.preview(id));
  }

  /**
   * Excludes invoices with a reason (RMTID.002 addendum).
   *
   * @param id batch
   * @param request invoices and reason
   * @return batch
   */
  @PostMapping("/{id}/exclude")
  @PreAuthorize(RemittanceAccess.EXCLUDE)
  public BatchResponse exclude(@PathVariable Long id, @Valid @RequestBody ExcludeRequest request) {
    return response(
        batches.exclude(id, request.invoiceNos(), request.reasonCode(), request.comment()));
  }

  /**
   * Restores an excluded invoice (RMTID.002 addendum).
   *
   * @param id batch
   * @param request invoice
   * @return batch
   */
  @PostMapping("/{id}/restore")
  @PreAuthorize(RemittanceAccess.EXCLUDE)
  public BatchResponse restore(@PathVariable Long id, @Valid @RequestBody RestoreRequest request) {
    return response(batches.restore(id, request.invoiceNo()));
  }

  /**
   * Submits for approval (RMTID.010).
   *
   * @param id batch
   * @param request comment
   * @return batch
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(RemittanceAccess.PROCESS)
  public BatchResponse submit(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return response(batches.submit(id, request.comment()));
  }

  /**
   * Approves and pushes to Disbursement (RMTID.010/011).
   *
   * @param id batch
   * @param request comment
   * @return batch
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(RemittanceAccess.APPROVE)
  public BatchResponse approve(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return response(batches.approve(id, request.comment()));
  }

  /**
   * Returns the batch with a reason (RMTID.029).
   *
   * @param id batch
   * @param request reason and comment
   * @return batch
   */
  @PostMapping("/{id}/return")
  @PreAuthorize(RemittanceAccess.RETURN)
  public BatchResponse returnBatch(
      @PathVariable Long id, @Valid @RequestBody ReturnRequest request) {
    return response(batches.returnBatch(id, request.reasonCode(), request.comment()));
  }

  /**
   * Re-assigns the processor (RMTID.009).
   *
   * @param id batch
   * @param request processor
   * @return batch
   */
  @PostMapping("/{id}/assign")
  @PreAuthorize(RemittanceAccess.ASSIGN)
  public BatchResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
    return response(batches.assign(id, request.username().strip()));
  }

  /**
   * The schedule (PDF / Excel) or payment request (RMTID.011): after submission a redirect to the
   * presigned link of the stored document, before it the document rendered now.
   *
   * @param id batch
   * @param kind document
   * @param request HTTP request (client address of the link audit)
   * @return redirect or file
   */
  @GetMapping("/{id}/documents/{kind}")
  @PreAuthorize(RemittanceAccess.BATCH_READ)
  public ResponseEntity<byte[]> document(
      @PathVariable Long id, @PathVariable DocumentKind kind, HttpServletRequest request) {
    return downloads.respond(store.download(visible(batches.get(id)), kind), request);
  }

  /**
   * Sends the schedule to the insurer, password-protected, once (MKTID.001).
   *
   * @param id batch
   * @param request e-mail
   * @return queued e-mail
   */
  @PostMapping("/{id}/send-schedule")
  @PreAuthorize(RemittanceAccess.PROCESS)
  public QueuedResponse sendSchedule(
      @PathVariable Long id, @Valid @RequestBody SendScheduleRequest request) {
    QueuedEmail queued =
        dispatch.send(
            id,
            new ScheduleDispatch.Mail(
                request.to(), request.cc(), request.subject(), request.body()));
    return new QueuedResponse(queued.messageId(), queued.passwordMessageId());
  }

  private BatchResponse response(RemittanceBatch batch) {
    return BatchResponse.from(batch, documents.insurerName(batch));
  }
}
