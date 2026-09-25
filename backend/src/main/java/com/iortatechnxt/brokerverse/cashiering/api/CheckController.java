package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.PdcReleaseRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.PdcRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.PdcResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.PickupRequestBody;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.PickupResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.Selection;
import com.iortatechnxt.brokerverse.cashiering.api.dto.SetupDtos.PrintBatchResponse;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PdcStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PickupStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItem.PdcCheck;
import com.iortatechnxt.brokerverse.cashiering.domain.PickupRequest.PickupDetails;
import com.iortatechnxt.brokerverse.cashiering.service.BatchPrintService;
import com.iortatechnxt.brokerverse.cashiering.service.PdcWarehouseService;
import com.iortatechnxt.brokerverse.cashiering.service.PickupService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Checks and printing (CSHID.008 item 4, CSHID.009, CSHID.019): the PDC warehouse with its maturity
 * run, the check pick-up queue with "Print ARs", and batch printing with preview, file download and
 * retry of failures.
 */
@RestController
@RequestMapping("/api/v1/cashiering")
public class CheckController {

  private final PdcWarehouseService pdcs;
  private final PickupService pickups;
  private final BatchPrintService printing;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param pdcs PDC warehouse
   * @param pickups pick-up queue
   * @param printing batch printing
   * @param clock clock
   */
  public CheckController(
      PdcWarehouseService pdcs, PickupService pickups, BatchPrintService printing, Clock clock) {
    this.pdcs = pdcs;
    this.pickups = pickups;
    this.printing = printing;
    this.clock = clock;
  }

  /**
   * Checks of the warehouse.
   *
   * @param companyId company
   * @param status status
   * @param from maturity from
   * @param to maturity to
   * @param page page
   * @param size size
   * @return checks by maturity
   */
  @GetMapping("/pdc")
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<PdcResponse> pdcs(
      @RequestParam Long companyId,
      @RequestParam(required = false) PdcStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        pdcs.list(companyId, status, from, to, CashAccess.page(page, size)), PdcResponse::from);
  }

  /**
   * Warehouses a check.
   *
   * @param r check
   * @return check
   */
  @PostMapping("/pdc")
  @PreAuthorize(CashAccess.UPLOAD)
  public PdcResponse warehouse(@Valid @RequestBody PdcRequest r) {
    return PdcResponse.from(
        pdcs.warehouse(
            r.companyId(),
            r.branchId(),
            new PdcCheck(
                null,
                r.clientCode(),
                r.payorName(),
                r.reference(),
                r.checkNo(),
                r.bankCode(),
                r.checkBranch(),
                r.maturityDate(),
                r.amount(),
                "PHP",
                r.segment())));
  }

  /**
   * Takes a check out of the warehouse.
   *
   * @param id check
   * @param r outcome and reason
   * @return check
   */
  @PostMapping("/pdc/{id}/release")
  @PreAuthorize(CashAccess.UPLOAD)
  public PdcResponse release(@PathVariable Long id, @Valid @RequestBody PdcReleaseRequest r) {
    return PdcResponse.from(pdcs.release(id, r.outcome(), r.reason()));
  }

  /**
   * Matures the checks due today now (the {@code PDC_MATURITY} job).
   *
   * @return checks matured
   */
  @PostMapping("/pdc/mature")
  @PreAuthorize(CashAccess.UPLOAD)
  public Map<String, Integer> mature() {
    return Map.of("matured", pdcs.mature(LocalDate.now(clock)));
  }

  /**
   * The pick-up queue.
   *
   * @param companyId company
   * @param status status
   * @param from pick-up date from
   * @param to pick-up date to
   * @param page page
   * @param size size
   * @return requests by pick-up date
   */
  @GetMapping("/pickups")
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<PickupResponse> pickups(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "FOR_PICKUP") PickupStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        pickups.list(companyId, status, from, to, CashAccess.page(page, size)),
        PickupResponse::from);
  }

  /**
   * Queues a pick-up request.
   *
   * @param r request
   * @return request
   */
  @PostMapping("/pickups")
  @PreAuthorize(CashAccess.RECEIPT)
  public PickupResponse queue(@Valid @RequestBody PickupRequestBody r) {
    return PickupResponse.from(
        pickups.create(
            r.companyId(),
            r.branchId(),
            new PickupDetails(
                r.collectionRef(),
                r.reference(),
                r.clientCode(),
                r.payorName(),
                null,
                r.pickupDate(),
                r.requestor(),
                r.amount(),
                "PHP",
                r.checkNo(),
                r.checkBank())));
  }

  /**
   * Pulls the pending requests of the Collection system.
   *
   * @param companyId company
   * @return requests queued
   */
  @PostMapping("/pickups/import")
  @PreAuthorize(CashAccess.RECEIPT)
  public Map<String, Integer> importPending(@RequestParam Long companyId) {
    return Map.of("queued", pickups.importPending(companyId));
  }

  /**
   * Prints the ARs of the selected requests that are due.
   *
   * @param selection requests
   * @return the print batch
   */
  @PostMapping("/pickups/print")
  @PreAuthorize(CashAccess.PRINT)
  public PrintBatchResponse printArs(@Valid @RequestBody Selection selection) {
    List<Long> receipts = pickups.printArs(selection.ids());
    return PrintBatchResponse.from(
        printing.print(selection.companyId(), receipts, "Check pick-up ARs"), true);
  }

  /**
   * Cancels a queued request.
   *
   * @param id request
   * @return request
   */
  @PostMapping("/pickups/{id}/cancel")
  @PreAuthorize(CashAccess.RECEIPT)
  public PickupResponse cancelPickup(@PathVariable Long id) {
    return PickupResponse.from(pickups.cancel(id));
  }

  /**
   * Print batches.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return batches, newest first
   */
  @GetMapping("/print-batches")
  @PreAuthorize(CashAccess.PRINT)
  public PageResponse<PrintBatchResponse> batches(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        printing.list(companyId, CashAccess.page(page, size)),
        b -> PrintBatchResponse.from(b, false));
  }

  /**
   * Prints receipts into one batch.
   *
   * @param selection receipts and how they were selected
   * @return the batch
   */
  @PostMapping("/print-batches")
  @PreAuthorize(CashAccess.PRINT)
  public PrintBatchResponse print(@Valid @RequestBody Selection selection) {
    return PrintBatchResponse.from(
        printing.print(
            selection.companyId(),
            selection.ids(),
            selection.criteria() == null ? "Selection" : selection.criteria()),
        true);
  }

  /**
   * One batch with its receipts.
   *
   * @param id batch
   * @return batch
   */
  @GetMapping("/print-batches/{id}")
  @PreAuthorize(CashAccess.PRINT)
  public PrintBatchResponse batch(@PathVariable Long id) {
    return PrintBatchResponse.from(printing.get(id), true);
  }

  /**
   * Prints the failures of a batch again.
   *
   * @param id batch
   * @return the new batch
   */
  @PostMapping("/print-batches/{id}/retry")
  @PreAuthorize(CashAccess.PRINT)
  public PrintBatchResponse retry(@PathVariable Long id) {
    return PrintBatchResponse.from(printing.retry(id), true);
  }

  /**
   * The merged PDF of a batch.
   *
   * @param id batch
   * @return PDF
   */
  @GetMapping("/print-batches/{id}/file")
  @PreAuthorize(CashAccess.PRINT)
  public ResponseEntity<byte[]> file(@PathVariable Long id) {
    var batch = printing.get(id);
    return CashAccess.pdf(batch.getBatchNo() + ".pdf", batch.document());
  }
}
