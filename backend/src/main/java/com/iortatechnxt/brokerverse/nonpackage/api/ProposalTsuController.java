package com.iortatechnxt.brokerverse.nonpackage.api;

import com.iortatechnxt.brokerverse.account.api.dto.CommentRequest;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.ComparativeView;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.HistoryView;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.InsurersBody;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.ProposalSlipBody;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.ResponseView;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.SlipSubmitBody;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.TermsBody;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalDtos.TermsCompleteBody;
import com.iortatechnxt.brokerverse.nonpackage.api.dto.ProposalResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.service.InsurerResponseService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalDocuments;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalQueryService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalSlipService;
import com.iortatechnxt.brokerverse.nonpackage.service.QuotationSlipService;
import jakarta.validation.Valid;
import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * The TSU side of a PRF (BRNB.007-010/017): insurer selection, quotation slip submission and
 * approval (sent to the insurers), insurer responses with their history and documents, the
 * recommended insurer, terms complete, the comparative table (JSON, PDF, Excel) and the proposal
 * slip submission and approval.
 */
@RestController
@RequestMapping("/api/v1/proposals/{id}")
public class ProposalTsuController {

  private static final String PROCESS = "hasAuthority('TSU_PROCESS')";
  private static final String APPROVE = "hasAuthority('TSU_APPROVE')";

  private final QuotationSlipService quotationSlips;
  private final InsurerResponseService responses;
  private final ProposalSlipService proposalSlips;
  private final ProposalDocuments documents;
  private final ProposalQueryService queries;

  /**
   * Creates the controller.
   *
   * @param quotationSlips quotation slip
   * @param responses insurer responses
   * @param proposalSlips proposal slip
   * @param documents comparative table files
   * @param queries PRF reads
   */
  public ProposalTsuController(
      QuotationSlipService quotationSlips,
      InsurerResponseService responses,
      ProposalSlipService proposalSlips,
      ProposalDocuments documents,
      ProposalQueryService queries) {
    this.quotationSlips = quotationSlips;
    this.responses = responses;
    this.proposalSlips = proposalSlips;
    this.documents = documents;
    this.queries = queries;
  }

  /**
   * Selects the insurers of the quotation slip.
   *
   * @param id PRF
   * @param body insurers
   * @return the PRF
   */
  @PutMapping("/insurers")
  @PreAuthorize(PROCESS)
  public ProposalResponse insurers(@PathVariable Long id, @Valid @RequestBody InsurersBody body) {
    return reload(quotationSlips.selectInsurers(id, body.insurers()));
  }

  /**
   * Submits the quotation slip for approval.
   *
   * @param id PRF
   * @param body reply date and comment
   * @return the PRF
   */
  @PostMapping("/quotation-slip/submit")
  @PreAuthorize(PROCESS)
  public ProposalResponse submitQuotationSlip(
      @PathVariable Long id, @Valid @RequestBody SlipSubmitBody body) {
    return reload(quotationSlips.submit(id, body.replyBy(), body.comment()));
  }

  /**
   * Approves the quotation slip and sends it to the insurers.
   *
   * @param id PRF
   * @param body comment
   * @return the PRF
   */
  @PostMapping("/quotation-slip/approve")
  @PreAuthorize(APPROVE)
  public ProposalResponse approveQuotationSlip(
      @PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(quotationSlips.approve(id, body.text()));
  }

  /**
   * The quotation slip PDF.
   *
   * @param id PRF
   * @return PDF
   */
  @GetMapping("/quotation-slip.pdf")
  @PreAuthorize(ProposalController.VIEW)
  public ResponseEntity<byte[]> quotationSlip(@PathVariable Long id) {
    return file(quotationSlips.pdf(id));
  }

  /**
   * Insurer responses.
   *
   * @param id PRF
   * @return responses
   */
  @GetMapping("/responses")
  @PreAuthorize(ProposalController.VIEW)
  public List<ResponseView> responses(@PathVariable Long id) {
    return responses.responses(id).stream().map(ResponseView::from).toList();
  }

  /**
   * Version history of the responses.
   *
   * @param id PRF
   * @return snapshots, newest first
   */
  @GetMapping("/responses/history")
  @PreAuthorize(ProposalController.VIEW)
  public List<HistoryView> history(@PathVariable Long id) {
    return responses.history(id).stream().map(HistoryView::from).toList();
  }

  /**
   * Records an insurer's terms.
   *
   * @param id PRF
   * @param responseId response
   * @param body terms
   * @return the response
   */
  @PutMapping("/responses/{responseId}")
  @PreAuthorize(PROCESS)
  public ResponseView record(
      @PathVariable Long id, @PathVariable Long responseId, @Valid @RequestBody TermsBody body) {
    return ResponseView.from(responses.record(id, responseId, body.terms()));
  }

  /**
   * Attaches the insurer's response document.
   *
   * @param id PRF
   * @param responseId response
   * @param file document
   * @return the response
   */
  @PostMapping("/responses/{responseId}/document")
  @PreAuthorize(PROCESS)
  public ResponseView attach(
      @PathVariable Long id,
      @PathVariable Long responseId,
      @RequestParam("file") MultipartFile file) {
    try {
      return ResponseView.from(
          responses.attach(
              id,
              responseId,
              new UploadedFile(
                  file.getOriginalFilename() == null ? "response" : file.getOriginalFilename(),
                  file.getBytes())));
    } catch (IOException e) {
      throw new BusinessRuleException("UPLOAD_FAILED", "The file could not be read", e);
    }
  }

  /**
   * Flags the recommended insurer.
   *
   * @param id PRF
   * @param responseId response
   * @return the response
   */
  @PostMapping("/responses/{responseId}/recommend")
  @PreAuthorize(PROCESS)
  public ResponseView recommend(@PathVariable Long id, @PathVariable Long responseId) {
    return ResponseView.from(responses.recommend(id, responseId));
  }

  /**
   * Closes the request for terms.
   *
   * @param id PRF
   * @param body close with pending responses, comment
   * @return the PRF
   */
  @PostMapping("/terms-complete")
  @PreAuthorize(PROCESS)
  public ProposalResponse termsComplete(
      @PathVariable Long id, @Valid @RequestBody TermsCompleteBody body) {
    return reload(responses.termsComplete(id, body.closePending(), body.comment()));
  }

  /**
   * The comparative table.
   *
   * @param id PRF
   * @return table
   */
  @GetMapping("/comparative")
  @PreAuthorize(ProposalController.VIEW)
  public ComparativeView comparative(@PathVariable Long id) {
    return ComparativeView.from(responses.comparative(id));
  }

  /**
   * The comparative table as PDF.
   *
   * @param id PRF
   * @return PDF
   */
  @GetMapping("/comparative.pdf")
  @PreAuthorize(ProposalController.VIEW)
  public ResponseEntity<byte[]> comparativePdf(@PathVariable Long id) {
    return file(documents.comparativePdf(queries.get(id), responses.responses(id)));
  }

  /**
   * The comparative table as Excel.
   *
   * @param id PRF
   * @return XLSX
   */
  @GetMapping("/comparative.xlsx")
  @PreAuthorize(ProposalController.VIEW)
  public ResponseEntity<byte[]> comparativeXlsx(@PathVariable Long id) {
    return file(documents.comparativeXlsx(queries.get(id), responses.responses(id)));
  }

  /**
   * Submits the proposal slip with the chosen insurer.
   *
   * @param id PRF
   * @param body insurer and comment
   * @return the PRF
   */
  @PostMapping("/proposal-slip/submit")
  @PreAuthorize(PROCESS)
  public ProposalResponse submitProposalSlip(
      @PathVariable Long id, @Valid @RequestBody ProposalSlipBody body) {
    return reload(proposalSlips.submit(id, body.insurerCode(), body.comment()));
  }

  /**
   * Approves the proposal slip and releases it to Marketing.
   *
   * @param id PRF
   * @param body comment
   * @return the PRF
   */
  @PostMapping("/proposal-slip/approve")
  @PreAuthorize(APPROVE)
  public ProposalResponse approveProposalSlip(
      @PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(proposalSlips.approve(id, body.text()));
  }

  /**
   * The current proposal slip PDF.
   *
   * @param id PRF
   * @return PDF
   */
  @GetMapping("/proposal-slip.pdf")
  @PreAuthorize(ProposalController.VIEW)
  public ResponseEntity<byte[]> proposalSlip(@PathVariable Long id) {
    return file(proposalSlips.pdf(id));
  }

  private ProposalResponse reload(ProposalRequest p) {
    ProposalRequest loaded = queries.get(p.getId());
    return ProposalResponse.from(loaded, queries.details(loaded));
  }

  private static ResponseEntity<byte[]> file(MessageFile f) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(f.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(f.fileName()))
        .body(f.content());
  }
}
