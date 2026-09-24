package com.iortatechnxt.brokerverse.issuance.api;

import com.iortatechnxt.brokerverse.issuance.api.dto.ConfirmPolicyRequest;
import com.iortatechnxt.brokerverse.issuance.api.dto.EpolicyResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.RejectRequest;
import com.iortatechnxt.brokerverse.issuance.api.dto.ReviewResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.UploadBatchResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.UploadChoiceRequest;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService.ReceivedFile;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyUploadConfirmation;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyUploadService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyUploadService.IncomingFile;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * E-policy receipt (BRNB.073): single upload matched to the account, bulk upload with match review,
 * extraction review, confirmation of the policy number(s) (BRNB.074/104) and rejection.
 */
@RestController
@RequestMapping("/api/v1/issuance")
public class EpolicyController {

  private final EpolicyService epolicies;
  private final EpolicyUploadService uploads;
  private final EpolicyUploadConfirmation confirmation;

  /**
   * Creates the controller.
   *
   * @param epolicies e-policy receipt and review
   * @param uploads bulk uploads
   * @param confirmation bulk upload confirmation
   */
  public EpolicyController(
      EpolicyService epolicies,
      EpolicyUploadService uploads,
      EpolicyUploadConfirmation confirmation) {
    this.epolicies = epolicies;
    this.uploads = uploads;
    this.confirmation = confirmation;
  }

  /**
   * Uploads one e-policy, matched by the ARN chosen, the policy number or the ARN in the file.
   *
   * @param companyId company
   * @param file PDF
   * @param arn account chosen, optional
   * @param policyNo policy number, optional
   * @return the e-policy
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/epolicies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(IssuanceController.MANAGE)
  public EpolicyResponse upload(
      @RequestParam Long companyId,
      @RequestParam MultipartFile file,
      @RequestParam(required = false) String arn,
      @RequestParam(required = false) String policyNo)
      throws IOException {
    return EpolicyResponse.from(
        epolicies.receive(
            new ReceivedFile(
                companyId, file.getOriginalFilename(), file.getBytes(), arn, policyNo)));
  }

  /**
   * Extraction review: extracted values next to the account values.
   *
   * @param id e-policy
   * @return review
   */
  @GetMapping("/epolicies/{id}")
  @PreAuthorize(IssuanceController.VIEW)
  public ReviewResponse review(@PathVariable Long id) {
    return ReviewResponse.from(epolicies.review(id));
  }

  /**
   * Extracts the policy data again.
   *
   * @param id e-policy
   * @return review
   */
  @PostMapping("/epolicies/{id}/extract")
  @PreAuthorize(IssuanceController.MANAGE)
  public ReviewResponse extract(@PathVariable Long id) {
    epolicies.reextract(id);
    return ReviewResponse.from(epolicies.review(id));
  }

  /**
   * Confirms the policy number(s): the account records the policy.
   *
   * @param id e-policy
   * @param request policy numbers and issue date
   * @return review
   */
  @PostMapping("/epolicies/{id}/confirm")
  @PreAuthorize(IssuanceController.MANAGE)
  public ReviewResponse confirm(
      @PathVariable Long id, @Valid @RequestBody ConfirmPolicyRequest request) {
    epolicies.confirm(id, request.policyNumbers(), request.issueDate());
    return ReviewResponse.from(epolicies.review(id));
  }

  /**
   * Rejects a received document.
   *
   * @param id e-policy
   * @param request reason and remarks
   * @return e-policy
   */
  @PostMapping("/epolicies/{id}/reject")
  @PreAuthorize(IssuanceController.MANAGE)
  public EpolicyResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
    return EpolicyResponse.from(epolicies.reject(id, request.reasonCode(), request.remarks()));
  }

  /**
   * Uploads many e-policies; the account of each is proposed for review.
   *
   * @param companyId company
   * @param files PDFs
   * @return the upload under review
   * @throws IOException when an upload cannot be read
   */
  @PostMapping(value = "/epolicy-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(IssuanceController.MANAGE)
  public UploadBatchResponse bulkUpload(
      @RequestParam Long companyId, @RequestParam("files") List<MultipartFile> files)
      throws IOException {
    List<IncomingFile> incoming = new ArrayList<>();
    for (MultipartFile f : files) {
      incoming.add(new IncomingFile(f.getOriginalFilename(), f.getBytes()));
    }
    return UploadBatchResponse.from(uploads.upload(companyId, incoming));
  }

  /**
   * One bulk upload with its match review.
   *
   * @param id upload
   * @return upload
   */
  @GetMapping("/epolicy-uploads/{id}")
  @PreAuthorize(IssuanceController.MANAGE)
  public UploadBatchResponse bulk(@PathVariable Long id) {
    return UploadBatchResponse.from(uploads.get(id));
  }

  /**
   * Changes the account of a file or leaves it out.
   *
   * @param id upload
   * @param itemId file
   * @param request account and inclusion
   * @return upload
   */
  @PostMapping("/epolicy-uploads/{id}/items/{itemId}")
  @PreAuthorize(IssuanceController.MANAGE)
  public UploadBatchResponse choose(
      @PathVariable Long id,
      @PathVariable Long itemId,
      @Valid @RequestBody UploadChoiceRequest request) {
    return UploadBatchResponse.from(uploads.choose(id, itemId, request.arn(), request.included()));
  }

  /**
   * Confirms a bulk upload: each included file is received on its account.
   *
   * @param id upload
   * @return upload with the outcome per file
   */
  @PostMapping("/epolicy-uploads/{id}/confirm")
  @PreAuthorize(IssuanceController.MANAGE)
  public UploadBatchResponse confirmBulk(@PathVariable Long id) {
    return UploadBatchResponse.from(confirmation.confirm(id));
  }

  /**
   * Discards a bulk upload.
   *
   * @param id upload
   * @return upload
   */
  @PostMapping("/epolicy-uploads/{id}/discard")
  @PreAuthorize(IssuanceController.MANAGE)
  public UploadBatchResponse discard(@PathVariable Long id) {
    return UploadBatchResponse.from(uploads.discard(id));
  }
}
