package com.iortatechnxt.brokerverse.disbursement.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.disbursement.api.dto.InstrumentDtos.CwtTagRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.InstrumentDtos.ReceiptTagRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.RequestDtos.ReasonComment;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.AllocationRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.BulkApproveRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.CommentRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.ItemResultDto;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.LineDto;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.LinesRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.TermsRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.VoucherResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.VoucherSummary;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher.VoucherTerms;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementForms;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementQueryService;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementQueryService.VoucherSearch;
import com.iortatechnxt.brokerverse.disbursement.service.FormFactsReader;
import com.iortatechnxt.brokerverse.disbursement.service.TagService;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherActions;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherBulk;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherService;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherViewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * Disbursement vouchers (DIS 2.7-2.21, 2.10-2.11, 2.16.6): lists by stage, the voucher page, its
 * terms and proforma entry (edit, reset, expense allocation), the workflow actions (submit, route,
 * submit for approval, approve single or multiple, reject, cancel), the DV document and the OR / AR
 * and CWT tags. The return to the processor runs through the workflow panel.
 */
@RestController
@RequestMapping("/api/v1/disbursement/vouchers")
public class VoucherController {

  private final VoucherService vouchers;
  private final VoucherActions actions;
  private final VoucherBulk bulk;
  private final VoucherViewService views;
  private final DisbursementQueryService queries;
  private final TagService tags;
  private final DisbursementForms forms;
  private final FormFactsReader facts;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param vouchers voucher processing
   * @param actions workflow actions
   * @param bulk bulk approval
   * @param views voucher page
   * @param queries lists
   * @param tags tags
   * @param forms DV document
   * @param facts form facts
   * @param currentUser current user
   */
  @SuppressWarnings("java:S107") // constructor injection
  public VoucherController(
      VoucherService vouchers,
      VoucherActions actions,
      VoucherBulk bulk,
      VoucherViewService views,
      DisbursementQueryService queries,
      TagService tags,
      DisbursementForms forms,
      FormFactsReader facts,
      CurrentUser currentUser) {
    this.vouchers = vouchers;
    this.actions = actions;
    this.bulk = bulk;
    this.views = views;
    this.queries = queries;
    this.tags = tags;
    this.forms = forms;
    this.facts = facts;
    this.currentUser = currentUser;
  }

  /**
   * Vouchers, newest first (DIS 2.4.4, 2.13.0).
   *
   * @param companyId company
   * @param stage stages
   * @param type disbursement type
   * @param q DV, payee or invoice
   * @param unregularized only vouchers whose posting failed
   * @param page page
   * @param size size
   * @return vouchers
   */
  @GetMapping
  @PreAuthorize(DisbursementAccess.READ)
  public PageResponse<VoucherSummary> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<VoucherStage> stage,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "false") boolean unregularized,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.vouchers(
            new VoucherSearch(companyId, stage, type, q, unregularized),
            DisbursementAccess.newestFirst(page, size)),
        VoucherSummary::from);
  }

  /**
   * The voucher page (DIS 2.7.3).
   *
   * @param id voucher
   * @return voucher
   */
  @GetMapping("/{id}")
  @PreAuthorize(DisbursementAccess.READ)
  public VoucherResponse get(@PathVariable Long id) {
    return page(id);
  }

  private VoucherResponse page(Long id) {
    return VoucherResponse.from(
        views.view(id), currentUser.hasAuthority(DisbursementAccess.VIEW_FULL));
  }

  /**
   * Sets the processing terms (DIS 2.7.1-2.7.4).
   *
   * @param id voucher
   * @param body terms
   * @return voucher
   */
  @PutMapping("/{id}/terms")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse terms(@PathVariable Long id, @Valid @RequestBody TermsRequest body) {
    vouchers.updateTerms(
        id,
        new VoucherTerms(
            body.mode(),
            body.bankAccountId(),
            body.payeeAccountId(),
            body.ewt(),
            body.purpose().strip(),
            body.valueDate(),
            body.costCenter(),
            body.expenseAccount()));
    return page(id);
  }

  /**
   * Saves the edited proforma entry (DIS 2.7.6).
   *
   * @param id voucher
   * @param body lines
   * @return voucher
   */
  @PutMapping("/{id}/proforma")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse proforma(@PathVariable Long id, @Valid @RequestBody LinesRequest body) {
    vouchers.editProforma(id, body.lines().stream().map(LineDto::values).toList());
    return page(id);
  }

  /**
   * Rebuilds the proforma from the rule.
   *
   * @param id voucher
   * @return voucher
   */
  @PostMapping("/{id}/proforma/reset")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse resetProforma(@PathVariable Long id) {
    vouchers.resetProforma(id);
    return page(id);
  }

  /**
   * Builds the expense lines from an allocation (DIS 2.7.10).
   *
   * @param id voucher
   * @param body rows
   * @return voucher
   */
  @PostMapping("/{id}/allocation")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse allocate(
      @PathVariable Long id, @Valid @RequestBody AllocationRequest body) {
    vouchers.allocate(id, body.allocations());
    return page(id);
  }

  /**
   * Submits for review (DIS 2.7.11).
   *
   * @param id voucher
   * @param body comment
   * @return voucher
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse submit(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    actions.submit(id, body.comment());
    return page(id);
  }

  /**
   * Routes straight to the approver (DIS 3.25.0).
   *
   * @param id voucher
   * @param body comment
   * @return voucher
   */
  @PostMapping("/{id}/route")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse route(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    actions.routeToApprover(id, body.comment());
    return page(id);
  }

  /**
   * Submits for approval (DIS 2.15.0).
   *
   * @param id voucher
   * @param body comment
   * @return voucher
   */
  @PostMapping("/{id}/submit-for-approval")
  @PreAuthorize(DisbursementAccess.REVIEW)
  public VoucherResponse submitForApproval(
      @PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    actions.submitForApproval(id, body.comment());
    return page(id);
  }

  /**
   * Approves and posts (DIS 2.19.0).
   *
   * @param id voucher
   * @param body comment
   * @return voucher
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(DisbursementAccess.APPROVE)
  public VoucherResponse approve(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    actions.approve(id, body.comment());
    return page(id);
  }

  /**
   * Approves several vouchers, one result each (DIS 2.19.0).
   *
   * @param body vouchers
   * @return results
   */
  @PostMapping("/approve")
  @PreAuthorize(DisbursementAccess.APPROVE)
  public List<ItemResultDto> approveAll(@Valid @RequestBody BulkApproveRequest body) {
    return bulk.approve(body.ids(), body.comment()).stream().map(ItemResultDto::from).toList();
  }

  /**
   * Rejects (DIS 2.21.0).
   *
   * @param id voucher
   * @param body reason
   * @return voucher
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(DisbursementAccess.APPROVE)
  public VoucherResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonComment body) {
    actions.reject(id, body.reasonCode(), body.comment());
    return page(id);
  }

  /**
   * Cancels in process, for review or approved (DIS 2.9.0, 2.18.0, 2.20.0).
   *
   * @param id voucher
   * @param body reason
   * @return voucher
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(DisbursementAccess.CANCEL)
  public VoucherResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonComment body) {
    actions.cancel(id, body.reasonCode(), body.comment());
    return page(id);
  }

  /**
   * The DV document (DIS 2.7.5, 2.16.6).
   *
   * @param id voucher
   * @return PDF
   */
  @GetMapping("/{id}/document")
  @PreAuthorize(DisbursementAccess.READ)
  public ResponseEntity<byte[]> document(@PathVariable Long id) {
    Voucher v = vouchers.get(id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(v.getDvNo() + ".pdf"))
        .contentType(MediaType.APPLICATION_PDF)
        .body(forms.voucher(v, facts.of(v)));
  }

  /**
   * Tags the OR / AR received (DIS 2.10.2).
   *
   * @param id voucher
   * @param body receipt
   * @return voucher
   */
  @PostMapping("/{id}/tags/receipt")
  @PreAuthorize(DisbursementAccess.TAG)
  public VoucherResponse receipt(
      @PathVariable Long id, @Valid @RequestBody ReceiptTagRequest body) {
    tags.receipt(id, body.tag());
    return page(id);
  }

  /**
   * Tags a CWT certificate (DIS 2.11.2).
   *
   * @param id voucher
   * @param body certificate
   * @return voucher
   */
  @PostMapping("/{id}/tags/cwt")
  @PreAuthorize(DisbursementAccess.TAG)
  public VoucherResponse cwt(@PathVariable Long id, @Valid @RequestBody CwtTagRequest body) {
    tags.cwt(id, body.tag());
    return page(id);
  }
}
