package com.iortatechnxt.brokerverse.disbursement.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.disbursement.api.dto.InstrumentDtos.EmailRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.InstrumentDtos.ReferenceRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.InstrumentDtos.ReleaseRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.InstrumentDtos.StatusEditRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.InstrumentDtos.StatusEditResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.RequestDtos.RequestResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.VoucherResponse;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentActions;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import com.iortatechnxt.brokerverse.disbursement.service.StatusEditService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The instrument of an approved voucher (DIS 2.7.7-2.7.9, 2.8.0-2.8.5, 2.16.2, 3.26.x): print the
 * check or form, download it, release a check or MC / DD, e-mail an ATD to the branch, confirm a
 * branch or BOB debit, receive an MC / DD, re-issue a stale check, and the status edits submitted
 * for the team leader's approval.
 */
@RestController
@RequestMapping("/api/v1/disbursement")
public class InstrumentController {

  private static final String VOUCHER_INSTRUMENT = "/vouchers/{id}/instrument";

  private final InstrumentActions actions;
  private final StatusEditService edits;
  private final VoucherViewService views;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param actions instrument actions
   * @param edits status edits
   * @param views voucher page
   * @param currentUser current user
   */
  public InstrumentController(
      InstrumentActions actions,
      StatusEditService edits,
      VoucherViewService views,
      CurrentUser currentUser) {
    this.actions = actions;
    this.edits = edits;
    this.views = views;
    this.currentUser = currentUser;
  }

  private VoucherResponse page(Long id) {
    return VoucherResponse.from(
        views.view(id), currentUser.hasAuthority(DisbursementAccess.VIEW_FULL));
  }

  /**
   * Prints the check or form (DIS 2.16.2, 3.26.3, 3.26.5-3.26.6).
   *
   * @param id voucher
   * @return voucher
   */
  @PostMapping(VOUCHER_INSTRUMENT + "/print")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse print(@PathVariable Long id) {
    actions.print(id, Change.user("Printed"));
    return page(id);
  }

  /**
   * The printed check or form.
   *
   * @param id voucher
   * @return PDF
   */
  @GetMapping(VOUCHER_INSTRUMENT + "/document")
  @PreAuthorize(DisbursementAccess.READ)
  public ResponseEntity<byte[]> document(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDispositions.attachment("instrument-" + id + ".pdf"))
        .contentType(MediaType.APPLICATION_PDF)
        .body(actions.document(id));
  }

  /**
   * Releases a check or MC / DD (DIS 2.8.1, 2.8.4).
   *
   * @param id voucher
   * @param body recipient
   * @return voucher
   */
  @PostMapping(VOUCHER_INSTRUMENT + "/release")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse release(@PathVariable Long id, @Valid @RequestBody ReleaseRequest body) {
    actions.release(id, body.releasedTo().strip());
    return page(id);
  }

  /**
   * E-mails the ATD to the processing branch (DIS 2.7.7, 2.8.2).
   *
   * @param id voucher
   * @param body recipients
   * @return voucher
   */
  @PostMapping(VOUCHER_INSTRUMENT + "/email")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse email(@PathVariable Long id, @Valid @RequestBody EmailRequest body) {
    actions.emailAtd(id, body.to(), body.cc() == null ? List.of() : body.cc());
    return page(id);
  }

  /**
   * Confirms the branch or BOB debit (DIS 2.8.2-2.8.3, 3.26.7).
   *
   * @param id voucher
   * @param body reference
   * @return voucher
   */
  @PostMapping(VOUCHER_INSTRUMENT + "/debited")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse debited(@PathVariable Long id, @Valid @RequestBody ReferenceRequest body) {
    actions.confirmDebit(id, body.reference());
    return page(id);
  }

  /**
   * Receives the MC / DD from the branch (DIS 2.8.4).
   *
   * @param id voucher
   * @param body MC / DD number
   * @return voucher
   */
  @PostMapping(VOUCHER_INSTRUMENT + "/received")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public VoucherResponse received(
      @PathVariable Long id, @Valid @RequestBody ReferenceRequest body) {
    actions.receive(id, body.reference());
    return page(id);
  }

  /**
   * Re-issues a stale check as a new request (design 6 row 10).
   *
   * @param id voucher of the stale check
   * @return the new request
   */
  @PostMapping(VOUCHER_INSTRUMENT + "/reissue")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public RequestResponse reissue(@PathVariable Long id) {
    return RequestResponse.from(actions.reissue(id));
  }

  /**
   * Requests a status correction (DIS 2.8.5).
   *
   * @param id voucher
   * @param body requested status and reason
   * @return the edit
   */
  @PostMapping(VOUCHER_INSTRUMENT + "/status-edits")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public StatusEditResponse requestEdit(
      @PathVariable Long id, @Valid @RequestBody StatusEditRequest body) {
    return StatusEditResponse.from(edits.request(id, body.toStatus(), body.reason()));
  }

  /**
   * Status edits waiting for approval.
   *
   * @return edits
   */
  @GetMapping("/status-edits")
  @PreAuthorize(DisbursementAccess.READ)
  public List<StatusEditResponse> pendingEdits() {
    return edits.pending().stream().map(StatusEditResponse::from).toList();
  }

  /**
   * Approves a status edit (DIS 2.8.5).
   *
   * @param editId edit
   * @return the edit
   */
  @PostMapping("/status-edits/{editId}/approve")
  @PreAuthorize(DisbursementAccess.STATUS_APPROVE)
  public StatusEditResponse approveEdit(@PathVariable Long editId) {
    return StatusEditResponse.from(edits.approve(editId));
  }
}
