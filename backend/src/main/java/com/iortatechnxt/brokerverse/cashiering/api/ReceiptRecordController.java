package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.api.RecordViews.HistoryEntry;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RecordDtos.CancellationRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RecordDtos.PostRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RecordDtos.RecordRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RecordDtos.RecordResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RecordDtos.ReinstatementRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RecordDtos.ReturnRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository.RecordCriteria;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster.Outcome;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordService;
import com.iortatechnxt.brokerverse.cashiering.service.RecordFormService;
import com.iortatechnxt.brokerverse.cashiering.service.RecordFormService.AccountRow;
import com.iortatechnxt.brokerverse.cashiering.service.RecordFormService.FormSettings;
import com.iortatechnxt.brokerverse.cashiering.service.ReversalRecordService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BDOI's AR / OR creation, cancellation and reinstatement records (Operations Cashiering FRS v3.2:
 * FRS.CSH.02.01, 02.02, 02.05, 03.01, 04.01): save, edit, cancel and submit by the cashier; the
 * lists For Posting and Returned; posting and return by the Approver/Poster.
 */
@RestController
@RequestMapping("/api/v1/cashiering/records")
public class ReceiptRecordController {

  private static final String CREATE =
      "hasAnyAuthority('CASH_RECEIPT', 'CASH_CANCEL', 'CASH_REINSTATE')";

  private static final int EXPORT_ROWS = 5000;

  private final ReceiptRecordService records;
  private final ReversalRecordService reversals;
  private final ReceiptRecordPoster poster;
  private final RecordViews views;
  private final RecordFormService form;

  /**
   * Creates the controller.
   *
   * @param records creation records
   * @param reversals cancellation and reinstatement records
   * @param poster posting and return
   * @param views labels and names of a record
   * @param form settings and account search of the creation screen
   */
  public ReceiptRecordController(
      ReceiptRecordService records,
      ReversalRecordService reversals,
      ReceiptRecordPoster poster,
      RecordViews views,
      RecordFormService form) {
    this.form = form;
    this.records = records;
    this.reversals = reversals;
    this.poster = poster;
    this.views = views;
  }

  /**
   * Records of a list (FRS.CSH.01.03.04, 02.05.01, 03.01.08.01, 04.01.09.01), newest first.
   *
   * @param companyId company
   * @param kind creation, cancellation or reinstatement
   * @param stages statuses, For Posting when none
   * @param filters receipt kind, type, branch, creator, dates, number and name
   * @param page page
   * @param size size
   * @return records
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<RecordResponse> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "CREATION") RecordKind kind,
      @RequestParam(required = false) Set<RecordStage> stages,
      ListFilters filters,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Set<RecordStage> wanted =
        stages == null || stages.isEmpty() ? EnumSet.of(RecordStage.FOR_POSTING) : stages;
    return PageResponse.of(
        records.search(companyId, kind, wanted, filters.criteria(), CashAccess.page(page, size)),
        views::of);
  }

  /**
   * Exports a list to Excel with the columns of the screen (FRS.CSH.01.03.04).
   *
   * @param companyId company
   * @param kind creation, cancellation or reinstatement
   * @param stages statuses, For Posting when none
   * @param filters filters of the list
   * @return xlsx file
   */
  @GetMapping("/export")
  @PreAuthorize(CashAccess.VIEW)
  public ResponseEntity<byte[]> export(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "CREATION") RecordKind kind,
      @RequestParam(required = false) Set<RecordStage> stages,
      ListFilters filters) {
    Set<RecordStage> wanted =
        stages == null || stages.isEmpty() ? EnumSet.of(RecordStage.FOR_POSTING) : stages;
    List<ReceiptRecord> rows =
        records
            .search(companyId, kind, wanted, filters.criteria(), PageRequest.of(0, EXPORT_ROWS))
            .getContent();
    return views.excel(kind, rows);
  }

  /**
   * One record.
   *
   * @param id record
   * @return record with its accounts
   */
  @GetMapping("/{id}")
  @PreAuthorize(CashAccess.VIEW)
  public RecordResponse get(@PathVariable Long id) {
    return views.of(records.get(id));
  }

  /**
   * The audit trail of a record (FRS.CSH.02.01.14): every activity with the user, date and time and
   * the values changed, newest first.
   *
   * @param id record
   * @return entries
   */
  @GetMapping("/{id}/history")
  @PreAuthorize(CashAccess.VIEW)
  public List<HistoryEntry> history(@PathVariable Long id) {
    return views.history(records.get(id));
  }

  /**
   * Saves an AR or OR creation record (FRS.CSH.02.01.08.02).
   *
   * @param request the record as entered
   * @return the record, status Created
   */
  @PostMapping
  @PreAuthorize(CashAccess.RECEIPT)
  public RecordResponse create(@Valid @RequestBody RecordRequest request) {
    return views.of(records.create(request.companyId(), request.draft()));
  }

  /**
   * Edits a creation record in status Created or Returned (FRS.CSH.02.01.13).
   *
   * @param id record
   * @param request the record as entered
   * @return the record
   */
  @PutMapping("/{id}")
  @PreAuthorize(CashAccess.RECEIPT)
  public RecordResponse edit(@PathVariable Long id, @Valid @RequestBody RecordRequest request) {
    return views.of(records.edit(id, request.draft()));
  }

  /**
   * Submits a record for posting (FRS.CSH.02.01.10).
   *
   * @param id record
   * @return the record, For Posting
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(CREATE)
  public RecordResponse submit(@PathVariable Long id) {
    return views.of(records.submit(id));
  }

  /**
   * Cancels a record in status Created (FRS.CSH.02.01.12).
   *
   * @param id record
   * @return the record, Record Cancelled
   */
  @PostMapping("/{id}/cancel-record")
  @PreAuthorize(CREATE)
  public RecordResponse cancelRecord(@PathVariable Long id) {
    return views.of(records.cancelRecord(id));
  }

  /**
   * Saves cancellation records, one per receipt (FRS.CSH.03.01.05).
   *
   * @param request receipts and reason
   * @return the records, status Created
   */
  @PostMapping("/cancellations")
  @PreAuthorize(CashAccess.CANCEL)
  public List<RecordResponse> cancellations(@Valid @RequestBody CancellationRequest request) {
    return reversals.cancel(request.receiptIds(), request.reason()).stream()
        .map(views::of)
        .toList();
  }

  /**
   * Saves reinstatement records, one per receipt (FRS.CSH.04.01.06).
   *
   * @param request receipts, type, accounts, reason and the information of CSHID.005
   * @return the records, status Created
   */
  @PostMapping("/reinstatements")
  @PreAuthorize(CashAccess.REINSTATE)
  public List<RecordResponse> reinstatements(@RequestBody ReinstatementRequest request) {
    return reversals.reinstate(request.receiptIds(), request.reason(), request.chosen()).stream()
        .map(views::of)
        .toList();
  }

  /**
   * Edits the reason of a cancellation or reinstatement record (FRS.CSH.03.01.07, 04.01.08).
   *
   * @param id record
   * @param request new reason (the receipts of the request are ignored)
   * @return the record
   */
  @PutMapping("/{id}/reason")
  @PreAuthorize("hasAnyAuthority('CASH_CANCEL', 'CASH_REINSTATE')")
  public RecordResponse editReason(
      @PathVariable Long id, @RequestBody ReinstatementRequest request) {
    return views.of(reversals.edit(id, request.reason(), request.chosen()));
  }

  /**
   * Posts the records selected (FRS.CSH.02.05.05, 03.01.08.06, 04.01.09.06).
   *
   * @param request records For Posting
   * @return the outcome of each record
   */
  @PostMapping("/post")
  @PreAuthorize(CashAccess.APPROVE)
  public List<Outcome> post(@Valid @RequestBody PostRequest request) {
    return poster.post(request.ids());
  }

  /**
   * Returns a record to its creator (FRS.CSH.02.05.07).
   *
   * @param id record For Posting
   * @param request reason
   * @return the record, Returned
   */
  @PostMapping("/{id}/return")
  @PreAuthorize(CashAccess.APPROVE)
  public RecordResponse returnToCreator(@PathVariable Long id, @RequestBody ReturnRequest request) {
    return views.of(poster.returnToCreator(id, request.reason()));
  }

  /**
   * Settings and lists of the creation screen: posting step, required remarks, holding period, bank
   * accounts with their defaults, receipting branches and the rate of the day.
   *
   * @param companyId company
   * @return settings
   */
  @GetMapping("/settings")
  @PreAuthorize(CashAccess.VIEW)
  public FormSettings settings(@RequestParam Long companyId) {
    return form.settings(companyId);
  }

  /**
   * Accounts whose number contains a text, with BDOI's account table (FRS.CSH.02.01.03).
   *
   * @param companyId company
   * @param q part of an account, invoice, ARN, policy or PN number
   * @return accounts
   */
  @GetMapping("/accounts")
  @PreAuthorize(CashAccess.VIEW)
  public List<AccountRow> accounts(@RequestParam Long companyId, @RequestParam String q) {
    return form.accounts(companyId, q);
  }

  /**
   * The creation records the current user saved on a day (FRS.CSH.02.01.16).
   *
   * @param companyId company
   * @param date day, today when none
   * @return records, oldest first
   */
  @GetMapping("/day")
  @PreAuthorize(CashAccess.VIEW)
  public List<RecordResponse> day(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
    LocalDate day = date == null ? views.today() : date;
    return records.dayList(companyId, day).stream().map(views::of).toList();
  }

  /**
   * Filters of a record list.
   *
   * @param receiptKind AR or OR
   * @param receiptType AR or OR type
   * @param branchId receipting branch
   * @param createdBy creator's user name
   * @param from created on or after
   * @param to created on or before
   * @param recordNo part of the record number
   * @param name part of the client, payor or insurer name
   */
  public record ListFilters(
      ReceiptKind receiptKind,
      String receiptType,
      Long branchId,
      String createdBy,
      @DateTimeFormat(iso = ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = ISO.DATE) LocalDate to,
      String recordNo,
      String name) {

    RecordCriteria criteria() {
      return new RecordCriteria(
          receiptKind,
          blank(receiptType),
          branchId,
          blank(createdBy),
          from == null ? null : BusinessClock.startOf(from),
          to == null ? null : BusinessClock.startOf(to.plusDays(1)),
          blank(recordNo),
          blank(name));
    }

    private static String blank(String value) {
      return value == null || value.isBlank() ? null : value.strip();
    }
  }
}
