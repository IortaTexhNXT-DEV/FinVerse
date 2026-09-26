package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.productmaint.api.dto.NegotiationDtos.ComparativeView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.NegotiationDtos.HistoryView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.NegotiationDtos.PrepareBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.NegotiationDtos.ResponseBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.NegotiationDtos.ResponseView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.NegotiationDtos.RoundView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.NegotiationDtos.SlipSubmitBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.NegotiationDtos.TermsFinalBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.CommentBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.RequestResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRound;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.service.NegotiationService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageDocuments;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequests;
import com.iortatechnxt.brokerverse.productmaint.service.PackageResponseService;
import com.iortatechnxt.brokerverse.productmaint.service.TermsCodec;
import com.iortatechnxt.brokerverse.productmaint.service.TermsService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * Insurer negotiation of a package request (BRPM.010/012-014, PMADD04): rounds and their quotation
 * slips (prepare, submit, four-eyes approval and send, resend, revise into a new round), insurer
 * responses with their documents and history, the live comparative table of a round, terms final,
 * the TSU Head's release of the terms and Marketing's acceptance.
 */
@RestController
@RequestMapping("/api/v1/product-maintenance/requests/{id}")
public class NegotiationController {

  private static final String NEGOTIATE = "hasAuthority('PKG_NEGOTIATE')";
  private static final String TSU_APPROVE = "hasAuthority('PKG_TSU_APPROVE')";

  private final NegotiationService negotiation;
  private final PackageResponseService responses;
  private final TermsService terms;
  private final PackageRequests requests;
  private final PackageDocuments documents;
  private final TermsCodec codec;

  /**
   * Creates the controller.
   *
   * @param negotiation rounds and slips
   * @param responses insurer responses
   * @param terms terms final and review
   * @param requests request loader
   * @param documents quotation slip PDF
   * @param codec terms JSON
   */
  public NegotiationController(
      NegotiationService negotiation,
      PackageResponseService responses,
      TermsService terms,
      PackageRequests requests,
      PackageDocuments documents,
      TermsCodec codec) {
    this.negotiation = negotiation;
    this.responses = responses;
    this.terms = terms;
    this.requests = requests;
    this.documents = documents;
    this.codec = codec;
  }

  /**
   * Rounds with their responses, first round first.
   *
   * @param id request
   * @return rounds
   */
  @GetMapping("/rounds")
  @PreAuthorize(PackageRequestController.VIEW)
  public List<RoundView> rounds(@PathVariable Long id) {
    return negotiation.rounds(id).stream().map(this::view).toList();
  }

  /**
   * Revises the quotation slip: closes the sent round and opens the next one (PMADD04).
   *
   * @param id request
   * @param body insurers and notes of the new round
   * @return the new round
   */
  @PostMapping("/rounds")
  @PreAuthorize(NEGOTIATE)
  public RoundView revise(@PathVariable Long id, @Valid @RequestBody PrepareBody body) {
    return view(negotiation.revise(id, body.insurers(), body.notes()));
  }

  /**
   * Changes the insurers and notes of a round's slip before it is sent.
   *
   * @param id request
   * @param roundNo round
   * @param body insurers and notes
   * @return the round
   */
  @PutMapping("/rounds/{roundNo}")
  @PreAuthorize(NEGOTIATE)
  public RoundView prepare(
      @PathVariable Long id, @PathVariable int roundNo, @Valid @RequestBody PrepareBody body) {
    return view(
        negotiation.prepare(
            id, roundNo, body.insurers() == null ? List.of() : body.insurers(), body.notes()));
  }

  /**
   * Submits a round's slip for approval.
   *
   * @param id request
   * @param roundNo round
   * @param body reply date
   * @return the round
   */
  @PostMapping("/rounds/{roundNo}/quotation-slip/submit")
  @PreAuthorize(NEGOTIATE)
  public RoundView submitSlip(
      @PathVariable Long id, @PathVariable int roundNo, @RequestBody SlipSubmitBody body) {
    return view(negotiation.submitSlip(id, roundNo, body.replyBy()));
  }

  /**
   * Approves a round's slip and sends it to the insurers (four eyes).
   *
   * @param id request
   * @param roundNo round
   * @return the round
   */
  @PostMapping("/rounds/{roundNo}/quotation-slip/approve")
  @PreAuthorize("hasAuthority('PKG_QS_APPROVE')")
  public RoundView approveSlip(@PathVariable Long id, @PathVariable int roundNo) {
    return view(negotiation.approveSlip(id, roundNo));
  }

  /**
   * The quotation slip PDF of a round.
   *
   * @param id request
   * @param roundNo round
   * @return PDF
   */
  @GetMapping("/rounds/{roundNo}/quotation-slip.pdf")
  @PreAuthorize(PackageRequestController.VIEW)
  public ResponseEntity<byte[]> slip(@PathVariable Long id, @PathVariable int roundNo) {
    NegotiationRound r = negotiation.round(id, roundNo);
    if (r.getQsNo() == null) {
      throw new BusinessRuleException(
          "QS_NOT_PREPARED", "The slip of this round is not numbered yet");
    }
    return PackageRequestController.file(documents.quotationSlip(requests.get(id), r));
  }

  /**
   * Resends a round's slip to one insurer.
   *
   * @param id request
   * @param roundNo round
   * @param insurerCode insurer
   * @return the insurer's response
   */
  @PostMapping("/rounds/{roundNo}/insurers/{insurerCode}/resend")
  @PreAuthorize(NEGOTIATE)
  public ResponseView resend(
      @PathVariable Long id, @PathVariable int roundNo, @PathVariable String insurerCode) {
    return ResponseView.from(negotiation.resend(id, roundNo, insurerCode), responses::terms);
  }

  /**
   * The live comparative table of a round.
   *
   * @param id request
   * @param roundNo round
   * @return table
   */
  @GetMapping("/rounds/{roundNo}/comparative")
  @PreAuthorize(PackageRequestController.VIEW)
  public ComparativeView comparative(@PathVariable Long id, @PathVariable int roundNo) {
    return ComparativeView.from(responses.comparative(negotiation.round(id, roundNo)));
  }

  /**
   * Records an insurer's outcome and terms.
   *
   * @param id request
   * @param responseId response
   * @param body terms
   * @return the response
   */
  @PutMapping("/responses/{responseId}")
  @PreAuthorize(NEGOTIATE)
  public ResponseView record(
      @PathVariable Long id, @PathVariable Long responseId, @Valid @RequestBody ResponseBody body) {
    return ResponseView.from(responses.record(id, responseId, body.toTerms()), responses::terms);
  }

  /**
   * Attaches an insurer's response document.
   *
   * @param id request
   * @param responseId response
   * @param file document
   * @return the response
   */
  @PostMapping("/responses/{responseId}/document")
  @PreAuthorize(NEGOTIATE)
  public ResponseView attach(
      @PathVariable Long id,
      @PathVariable Long responseId,
      @RequestParam("file") MultipartFile file) {
    try {
      UploadedFile upload =
          new UploadedFile(
              file.getOriginalFilename() == null ? "response" : file.getOriginalFilename(),
              file.getBytes());
      return ResponseView.from(responses.attach(id, responseId, upload), responses::terms);
    } catch (IOException e) {
      throw new BusinessRuleException("UPLOAD_FAILED", "The file could not be read", e);
    }
  }

  /**
   * Revision history of the responses.
   *
   * @param id request
   * @return snapshots, newest first
   */
  @GetMapping("/responses/history")
  @PreAuthorize(PackageRequestController.VIEW)
  public List<HistoryView> history(@PathVariable Long id) {
    return responses.history(id).stream().map(HistoryView::from).toList();
  }

  /**
   * Terms final: chooses the insurers and compiles the comparative master.
   *
   * @param id request
   * @param body chosen insurers and comment
   * @return the request
   */
  @PostMapping("/terms-final")
  @PreAuthorize(NEGOTIATE)
  public RequestResponse termsFinal(
      @PathVariable Long id, @Valid @RequestBody TermsFinalBody body) {
    return view(terms.termsFinal(id, body.insurers(), body.comment()));
  }

  /**
   * Releases the negotiated terms to Marketing (client-specific package).
   *
   * @param id request
   * @param body comment
   * @return the request
   */
  @PostMapping("/release-to-marketing")
  @PreAuthorize(TSU_APPROVE)
  public RequestResponse releaseToMarketing(
      @PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return view(terms.releaseToMarketing(id, body.text()));
  }

  /**
   * Moves a generic programme's terms straight to the requirements.
   *
   * @param id request
   * @param body comment
   * @return the request
   */
  @PostMapping("/skip-marketing-review")
  @PreAuthorize(TSU_APPROVE)
  public RequestResponse skipMarketingReview(
      @PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return view(terms.skipMarketingReview(id, body.text()));
  }

  /**
   * Marketing accepts the negotiated terms.
   *
   * @param id request
   * @param body comment
   * @return the request
   */
  @PostMapping("/accept-terms")
  @PreAuthorize("hasAuthority('PKG_REQUEST')")
  public RequestResponse acceptTerms(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return view(terms.acceptTerms(id, body.text()));
  }

  private RoundView view(NegotiationRound r) {
    return RoundView.from(
        r, responses.ofRound(r).stream().map(x -> ResponseView.from(x, responses::terms)).toList());
  }

  private RequestResponse view(PackageRequest p) {
    return PackageRequestController.view(p, codec);
  }
}
