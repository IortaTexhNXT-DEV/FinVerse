package com.iortatechnxt.brokerverse.booking.api;

import com.iortatechnxt.brokerverse.booking.api.dto.BatchConfirmRequest;
import com.iortatechnxt.brokerverse.booking.api.dto.BatchRunResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.BookRequest;
import com.iortatechnxt.brokerverse.booking.api.dto.BookingPreviewResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.EnqueueResultResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.QueueEditRequest;
import com.iortatechnxt.brokerverse.booking.api.dto.QueueEntryResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.QueueRequest;
import com.iortatechnxt.brokerverse.booking.api.dto.WorkbenchCountsResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.WorkbenchRowResponse;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.QueueSource;
import com.iortatechnxt.brokerverse.booking.service.BookingPreviewService;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.booking.service.BookingQueueService;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService;
import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService.Filter;
import com.iortatechnxt.brokerverse.booking.service.BookingWorkbenchService.Tab;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking Workbench and booking actions (BRNB.027/036/038/061/076/111): tiles and tabs, pre-booking
 * confirmation, individual booking, queue (add, edit, remove), batch confirm / cancel, "Book now"
 * and batch runs.
 */
@RestController
@RequestMapping("/api/v1/booking")
public class BookingController {

  private final BookingWorkbenchService workbench;
  private final BookingPreviewService preview;
  private final BookingService booking;
  private final BookingQueueService queue;
  private final BookingQueryService queries;

  /**
   * Creates the controller.
   *
   * @param workbench workbench reads
   * @param preview pre-booking confirmation
   * @param booking individual booking
   * @param queue queue and batches
   * @param queries invoice reads
   */
  public BookingController(
      BookingWorkbenchService workbench,
      BookingPreviewService preview,
      BookingService booking,
      BookingQueueService queue,
      BookingQueryService queries) {
    this.workbench = workbench;
    this.preview = preview;
    this.booking = booking;
    this.queue = queue;
    this.queries = queries;
  }

  /**
   * Workbench tiles.
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/workbench/counts")
  @PreAuthorize(BookingAccess.VIEW)
  public WorkbenchCountsResponse counts(@RequestParam Long companyId) {
    return WorkbenchCountsResponse.from(workbench.counts(companyId));
  }

  /**
   * Rows of a workbench tab.
   *
   * @param companyId company
   * @param tab READY, QUEUED, BOOKED or FAILED
   * @param q proposal number (ARN), client code or name
   * @param line product line
   * @param page page
   * @param size size
   * @return rows
   */
  @GetMapping("/workbench")
  @PreAuthorize(BookingAccess.VIEW)
  public PageResponse<WorkbenchRowResponse> rows(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "READY") Tab tab,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String line,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        workbench.rows(companyId, tab, new Filter(q, line), pageOf(page, size)),
        WorkbenchRowResponse::from);
  }

  /**
   * Pre-booking confirmation: invoice(s) and journal, nothing posted (BRNB.036).
   *
   * @param request account and choices
   * @return preview
   */
  @PostMapping("/preview")
  @PreAuthorize(BookingAccess.PROCESS)
  public BookingPreviewResponse preview(@Valid @RequestBody BookRequest request) {
    return BookingPreviewResponse.from(preview.preview(request.arn(), request.toOptions()));
  }

  /**
   * Books an account individually (BRNB.027).
   *
   * @param request account and choices
   * @return invoice id and number
   */
  @PostMapping("/book")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(BookingAccess.PROCESS)
  public Map<String, Object> book(@Valid @RequestBody BookRequest request) {
    BookedInvoice invoice =
        booking.book(request.arn(), request.toOptions(), BookingSource.INDIVIDUAL);
    return Map.of("id", invoice.getId(), "invoiceNo", invoice.getInvoiceNo());
  }

  /**
   * Adds accounts to the booking queue ("Add to batch").
   *
   * @param request company and accounts
   * @return result per account
   */
  @PostMapping("/queue")
  @PreAuthorize(BookingAccess.PROCESS)
  public List<EnqueueResultResponse> enqueue(@Valid @RequestBody QueueRequest request) {
    return queue.enqueue(request.companyId(), request.arns(), QueueSource.MANUAL).stream()
        .map(EnqueueResultResponse::from)
        .toList();
  }

  /**
   * Changes the booking date and cost center of a queued account.
   *
   * @param id queue entry
   * @param request date and cost center
   * @return entry
   */
  @PutMapping("/queue/{id}")
  @PreAuthorize(BookingAccess.PROCESS)
  public QueueEntryResponse edit(
      @PathVariable Long id, @Valid @RequestBody QueueEditRequest request) {
    return QueueEntryResponse.from(queue.edit(id, request.bookingDate(), request.costCenter()));
  }

  /**
   * Removes an account from the queue.
   *
   * @param id queue entry
   * @return entry
   */
  @PostMapping("/queue/{id}/remove")
  @PreAuthorize(BookingAccess.PROCESS)
  public QueueEntryResponse remove(@PathVariable Long id) {
    return QueueEntryResponse.from(queue.remove(id));
  }

  /**
   * Confirms the batch: books the queued accounts.
   *
   * @param request company, entries and business date
   * @return batch run
   */
  @PostMapping("/batch/confirm")
  @PreAuthorize(BookingAccess.PROCESS)
  public BatchRunResponse confirm(@Valid @RequestBody BatchConfirmRequest request) {
    return BatchRunResponse.from(
        queue.confirmBatch(
            request.companyId(),
            request.entryIds() == null ? List.of() : request.entryIds(),
            request.businessDate()));
  }

  /**
   * Cancels the batch: removes every queued account.
   *
   * @param companyId company
   * @return accounts removed
   */
  @PostMapping("/batch/cancel")
  @PreAuthorize(BookingAccess.PROCESS)
  public Map<String, Integer> cancel(@RequestParam Long companyId) {
    return Map.of("removed", queue.cancelBatch(companyId));
  }

  /**
   * Books a selection now, each account in its own transaction ("Book now").
   *
   * @param request company, accounts and booking date
   * @return batch run
   */
  @PostMapping("/book-now")
  @PreAuthorize(BookingAccess.PROCESS)
  public BatchRunResponse bookNow(@Valid @RequestBody QueueRequest request) {
    return BatchRunResponse.from(
        queue.bookNow(request.companyId(), request.arns(), request.bookingDate()));
  }

  /**
   * Batch runs.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return runs, newest first
   */
  @GetMapping("/batch-runs")
  @PreAuthorize(BookingAccess.VIEW)
  public PageResponse<BatchRunResponse> runs(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(queries.runs(companyId, pageOf(page, size)), BatchRunResponse::summary);
  }

  /**
   * One batch run with its per-account results.
   *
   * @param runNo run number
   * @return run
   */
  @GetMapping("/batch-runs/{runNo}")
  @PreAuthorize(BookingAccess.VIEW)
  public BatchRunResponse run(@PathVariable String runNo) {
    return BatchRunResponse.from(queries.run(runNo));
  }

  static PageRequest pageOf(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), BookingAccess.MAX_PAGE));
  }
}
