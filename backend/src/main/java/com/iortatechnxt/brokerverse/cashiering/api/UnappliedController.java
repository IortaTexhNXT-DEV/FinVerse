package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.ReasonBody;
import com.iortatechnxt.brokerverse.cashiering.api.dto.UnappliedDtos.BulkRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.UnappliedDtos.BulkResult;
import com.iortatechnxt.brokerverse.cashiering.api.dto.UnappliedDtos.DispositionRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.UnappliedDtos.DispositionResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.UnappliedDtos.DispositionTypeResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.UnappliedDtos.UnappliedResponse;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.DispositionDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.DispositionService;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
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
 * The unapplied payments workbench (CSHID.024/025): tabs Unapplied / Monitoring / For Approval /
 * For Reversal (and Done), the item with its disposition history, assign / update / submit /
 * approve / withdraw a disposition, mark and approve a reversal, and bulk submit / approve.
 */
@RestController
@RequestMapping("/api/v1/cashiering")
public class UnappliedController {

  private final UnappliedService unapplied;
  private final DispositionService dispositions;

  /**
   * Creates the controller.
   *
   * @param unapplied unapplied items
   * @param dispositions dispositions
   */
  public UnappliedController(UnappliedService unapplied, DispositionService dispositions) {
    this.unapplied = unapplied;
    this.dispositions = dispositions;
  }

  /**
   * Items of a tab.
   *
   * @param companyId company
   * @param tab UNAPPLIED, MONITORING, FOR_APPROVAL, FOR_REVERSAL or DONE
   * @param q search text
   * @param page page
   * @param size size
   * @return items, newest first
   */
  @GetMapping("/unapplied")
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<UnappliedResponse> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "UNAPPLIED") String tab,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        dispositions.list(companyId, stages(tab), q, CashAccess.page(page, size)),
        u -> UnappliedResponse.from(u, null));
  }

  /**
   * One item with its current disposition.
   *
   * @param id item
   * @return item
   */
  @GetMapping("/unapplied/{id}")
  @PreAuthorize(CashAccess.VIEW)
  public UnappliedResponse get(@PathVariable Long id) {
    Unapplied item = unapplied.get(id);
    List<Disposition> history = dispositions.history(id);
    return UnappliedResponse.from(item, history.isEmpty() ? null : history.get(history.size() - 1));
  }

  /**
   * Dispositions of an item, oldest first.
   *
   * @param id item
   * @return dispositions
   */
  @GetMapping("/unapplied/{id}/dispositions")
  @PreAuthorize(CashAccess.VIEW)
  public List<DispositionResponse> history(@PathVariable Long id) {
    return dispositions.history(id).stream().map(DispositionResponse::from).toList();
  }

  /**
   * Disposition types with their rule.
   *
   * @return types
   */
  @GetMapping("/disposition-types")
  @PreAuthorize(CashAccess.VIEW)
  public List<DispositionTypeResponse> types() {
    return dispositions.types().stream().map(DispositionTypeResponse::from).toList();
  }

  /**
   * Assigns a disposition.
   *
   * @param id item
   * @param request disposition
   * @return disposition
   */
  @PostMapping("/unapplied/{id}/disposition")
  @PreAuthorize(CashAccess.DISPOSITION)
  public DispositionResponse assign(
      @PathVariable Long id, @Valid @RequestBody DispositionRequest request) {
    return DispositionResponse.from(
        dispositions.assign(id, request.dispositionType(), details(request)));
  }

  /**
   * Updates the disposition before it is processed.
   *
   * @param id item
   * @param request disposition
   * @return disposition
   */
  @PutMapping("/unapplied/{id}/disposition")
  @PreAuthorize(CashAccess.DISPOSITION)
  public DispositionResponse update(
      @PathVariable Long id, @Valid @RequestBody DispositionRequest request) {
    return DispositionResponse.from(
        dispositions.update(id, request.dispositionType(), details(request)));
  }

  /**
   * Submits (or processes) the disposition.
   *
   * @param id item
   * @return disposition
   */
  @PostMapping("/unapplied/{id}/submit")
  @PreAuthorize(CashAccess.DISPOSITION)
  public DispositionResponse submit(@PathVariable Long id) {
    return DispositionResponse.from(dispositions.submit(id));
  }

  /**
   * Approves and processes the disposition.
   *
   * @param id item
   * @return disposition
   */
  @PostMapping("/unapplied/{id}/approve")
  @PreAuthorize(CashAccess.DISPOSITION_APPROVE)
  public DispositionResponse approve(@PathVariable Long id) {
    return DispositionResponse.from(dispositions.approve(id));
  }

  /**
   * Withdraws the disposition.
   *
   * @param id item
   * @return item
   */
  @PostMapping("/unapplied/{id}/withdraw")
  @PreAuthorize(CashAccess.DISPOSITION)
  public UnappliedResponse withdraw(@PathVariable Long id) {
    return UnappliedResponse.from(dispositions.withdraw(id), null);
  }

  /**
   * Marks a completed disposition for reversal.
   *
   * @param id item
   * @param body reason
   * @return disposition
   */
  @PostMapping("/unapplied/{id}/reversal")
  @PreAuthorize(CashAccess.DISPOSITION)
  public DispositionResponse markReversal(
      @PathVariable Long id, @Valid @RequestBody ReasonBody body) {
    return DispositionResponse.from(dispositions.markReversal(id, body.reason()));
  }

  /**
   * Approves a reversal.
   *
   * @param id item
   * @return disposition
   */
  @PostMapping("/unapplied/{id}/reversal/approve")
  @PreAuthorize(CashAccess.DISPOSITION_APPROVE)
  public DispositionResponse approveReversal(@PathVariable Long id) {
    return DispositionResponse.from(dispositions.approveReversal(id));
  }

  /**
   * Submits several dispositions.
   *
   * @param request items
   * @return outcome per item
   */
  @PostMapping("/unapplied/bulk/submit")
  @PreAuthorize(CashAccess.DISPOSITION)
  public BulkResult bulkSubmit(@Valid @RequestBody BulkRequest request) {
    return bulk(request.ids(), dispositions::submit);
  }

  /**
   * Approves several dispositions.
   *
   * @param request items
   * @return outcome per item
   */
  @PostMapping("/unapplied/bulk/approve")
  @PreAuthorize(CashAccess.DISPOSITION_APPROVE)
  public BulkResult bulkApprove(@Valid @RequestBody BulkRequest request) {
    return bulk(request.ids(), dispositions::approve);
  }

  private BulkResult bulk(List<Long> ids, LongConsumer action) {
    List<String> done = new ArrayList<>();
    List<String> failures = new ArrayList<>();
    for (Long id : ids) {
      String reference = unapplied.get(id).getReference();
      try {
        action.accept(id);
        done.add(reference);
      } catch (BusinessRuleException ex) {
        failures.add(reference + ": " + ex.getMessage());
      }
    }
    return new BulkResult(done, failures);
  }

  private static DispositionDetails details(DispositionRequest r) {
    return new DispositionDetails(
        r.amount(),
        r.targetInvoiceNo(),
        r.targetClientCode(),
        r.targetUnit(),
        r.payeeName(),
        r.remarks());
  }

  private static List<String> stages(String tab) {
    return switch (tab) {
      case "MONITORING" -> DispositionService.TAB_MONITORING;
      case "FOR_APPROVAL" -> DispositionService.TAB_FOR_APPROVAL;
      case "FOR_REVERSAL" -> DispositionService.TAB_FOR_REVERSAL;
      case "DONE" -> DispositionService.TAB_DONE;
      default -> DispositionService.TAB_UNAPPLIED;
    };
  }
}
