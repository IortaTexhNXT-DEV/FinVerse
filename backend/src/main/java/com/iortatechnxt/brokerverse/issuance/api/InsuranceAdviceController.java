package com.iortatechnxt.brokerverse.issuance.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.AdviceResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.AdviceSendRequest;
import com.iortatechnxt.brokerverse.issuance.api.dto.ArnsRequest;
import com.iortatechnxt.brokerverse.issuance.service.AdviceDispatchService;
import com.iortatechnxt.brokerverse.issuance.service.InsuranceAdviceService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceBatchService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceBatchService.Outcome;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
 * Insurance Advice register (BRNB.060/070/095/035): search and list, view and download, generate
 * single or multiple, send single or multiple password protected.
 */
@RestController
@RequestMapping("/api/v1/issuance/insurance-advice")
public class InsuranceAdviceController {

  private static final String SEND =
      "hasAnyAuthority('EPOLICY_MANAGE', 'EPOLICY_SEND', 'ACCOUNT_MAINTAIN')";

  private final InsuranceAdviceService advices;
  private final AdviceDispatchService dispatch;
  private final IssuanceBatchService batch;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param advices insurance advices
   * @param dispatch sending
   * @param batch generation for several accounts
   * @param downloads file download answers
   */
  public InsuranceAdviceController(
      InsuranceAdviceService advices,
      AdviceDispatchService dispatch,
      IssuanceBatchService batch,
      FileDownloads downloads) {
    this.downloads = downloads;
    this.advices = advices;
    this.dispatch = dispatch;
    this.batch = batch;
  }

  /**
   * The register, newest first.
   *
   * @param companyId company
   * @param text IA number, ARN or client fragment
   * @param page page
   * @param size size
   * @return advices
   */
  @GetMapping
  @PreAuthorize(IssuanceController.VIEW)
  public PageResponse<AdviceResponse> register(
      @RequestParam Long companyId,
      @RequestParam(required = false) String text,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        advices.register(companyId, text, IssuanceController.pageable(page, size)),
        AdviceResponse::from);
  }

  /**
   * One advice.
   *
   * @param id advice
   * @return advice
   */
  @GetMapping("/{id}")
  @PreAuthorize(IssuanceController.VIEW)
  public AdviceResponse get(@PathVariable Long id) {
    return AdviceResponse.from(advices.get(id));
  }

  /**
   * Downloads the PDF of an advice: a redirect to its presigned link, or the bytes of an advice
   * generated before ST1.
   *
   * @param id advice
   * @param request HTTP request (client address of the link audit)
   * @return redirect or PDF
   */
  @GetMapping("/{id}/file")
  @PreAuthorize(IssuanceController.VIEW)
  public ResponseEntity<byte[]> file(@PathVariable Long id, HttpServletRequest request) {
    return downloads.respond(advices.download(id), request);
  }

  /**
   * Generates the advice of one or more mortgaged accounts.
   *
   * @param request accounts
   * @return outcome per account (IA number when generated)
   */
  @PostMapping("/generate")
  @PreAuthorize(IssuanceController.MANAGE)
  public List<Outcome> generate(@Valid @RequestBody ArnsRequest request) {
    return batch.generateAdvices(request.arns());
  }

  /**
   * Sends one or more advices, protected with a password sent separately.
   *
   * @param request advices, recipients and password hint
   * @return advices sent
   */
  @PostMapping("/send")
  @PreAuthorize(SEND)
  public List<AdviceResponse> send(@Valid @RequestBody AdviceSendRequest request) {
    return dispatch.send(request.toEmail()).stream().map(AdviceResponse::from).toList();
  }
}
