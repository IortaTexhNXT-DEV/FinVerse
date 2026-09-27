package com.iortatechnxt.brokerverse.csf.api;

import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.csf.api.dto.CsfRequests;
import com.iortatechnxt.brokerverse.csf.service.ContactChangeService;
import com.iortatechnxt.brokerverse.csf.service.CsfDocumentService;
import com.iortatechnxt.brokerverse.csf.service.CsfDocumentService.Upload;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ChangeView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.DocumentView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ResendPreview;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ResendResult;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.VerificationView;
import com.iortatechnxt.brokerverse.csf.service.ResendService;
import com.iortatechnxt.brokerverse.csf.service.VerificationService;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * Actions of the Customer Servicing Facility on a client (FR-CSF-020, 021, 030 to 033): caller
 * verification and contact change under {@code CSF_CONTACT_UPDATE}, referral to the fulfilment
 * unit, resends under {@code CSF_RESEND} (another address also needs {@code CSF_RESEND_OTHER}),
 * uploads under {@code CSF_DOCUMENT_UPLOAD}, and logged downloads under {@code CSF_VIEW} with
 * {@code ATTACHMENT_VIEW}.
 */
@RestController
@RequestMapping("/api/v1/csf/clients/{clientId}")
public class CsfActionController {

  private final VerificationService verifications;
  private final ContactChangeService changes;
  private final ResendService resends;
  private final CsfDocumentService documents;
  private final AttachmentService attachments;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param verifications caller verifications
   * @param changes contact changes and referrals
   * @param resends resends
   * @param documents documents
   * @param attachments upload size limit
   * @param downloads file answers
   */
  public CsfActionController(
      VerificationService verifications,
      ContactChangeService changes,
      ResendService resends,
      CsfDocumentService documents,
      AttachmentService attachments,
      FileDownloads downloads) {
    this.verifications = verifications;
    this.changes = changes;
    this.resends = resends;
    this.documents = documents;
    this.attachments = attachments;
    this.downloads = downloads;
  }

  /**
   * Records the verification of the caller.
   *
   * @param companyId company
   * @param clientId client
   * @param request channel and checks
   * @return the verification
   */
  @PostMapping("/verifications")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('CSF_CONTACT_UPDATE')")
  public VerificationView verify(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @Valid @RequestBody CsfRequests.Verify request) {
    return verifications.verify(companyId, clientId, request.toRequest());
  }

  /**
   * Applies a contact change after a passed verification.
   *
   * @param companyId company
   * @param clientId client
   * @param request verification, reason and new values
   * @return the change
   */
  @PostMapping("/contact-changes")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('CSF_CONTACT_UPDATE')")
  public ChangeView change(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @Valid @RequestBody CsfRequests.Change request) {
    return changes.apply(companyId, clientId, request.toRequest());
  }

  /**
   * Refers a change the contact centre cannot make to the fulfilment unit.
   *
   * @param companyId company
   * @param clientId client
   * @param request fields asked for and remarks
   * @return the referral
   */
  @PostMapping("/referrals")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('CSF_CONTACT_UPDATE')")
  public ChangeView refer(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @Valid @RequestBody CsfRequests.Referral request) {
    return changes.refer(companyId, clientId, request.toRequest());
  }

  /**
   * What a resend will send.
   *
   * @param companyId company
   * @param clientId client
   * @param kind RA or EPOLICY
   * @param documentId attachment or e-policy
   * @return preview
   */
  @GetMapping("/resend-preview")
  @PreAuthorize("hasAuthority('CSF_RESEND')")
  public ResendPreview preview(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @RequestParam ResendService.Kind kind,
      @RequestParam Long documentId) {
    return resends.preview(companyId, clientId, kind, documentId);
  }

  /**
   * Resends a renewal advice.
   *
   * @param companyId company
   * @param clientId client
   * @param request renewal advice, recipient and reason
   * @return what was sent
   */
  @PostMapping("/resend-advice")
  @PreAuthorize("hasAuthority('CSF_RESEND')")
  public ResendResult resendAdvice(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @Valid @RequestBody CsfRequests.Resend request) {
    return resends.resendAdvice(companyId, clientId, request.toRequest());
  }

  /**
   * Resends a confirmed e-policy.
   *
   * @param companyId company
   * @param clientId client
   * @param request e-policy, recipient and reason
   * @return what was sent
   */
  @PostMapping("/resend-epolicy")
  @PreAuthorize("hasAuthority('CSF_RESEND')")
  public ResendResult resendEpolicy(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @Valid @RequestBody CsfRequests.Resend request) {
    return resends.resendEpolicy(companyId, clientId, request.toRequest());
  }

  /**
   * Uploads a document to the client or one of its accounts.
   *
   * @param companyId company
   * @param clientId client
   * @param accountId account of the client, empty for the client itself
   * @param documentType document type (list CSF_DOCUMENT_TYPE)
   * @param description description
   * @param file file
   * @return the stored document
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('CSF_DOCUMENT_UPLOAD')")
  public DocumentView upload(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @RequestParam(required = false) Long accountId,
      @RequestParam String documentType,
      @RequestParam(required = false) String description,
      @RequestParam MultipartFile file)
      throws IOException {
    attachments.requireWithinLimit(file.getSize());
    return documents.upload(
        companyId,
        clientId,
        new Upload(
            accountId,
            documentType,
            description,
            new UploadedFile(file.getOriginalFilename(), file.getBytes())));
  }

  /**
   * Downloads a document of the client's records (logged).
   *
   * @param companyId company
   * @param clientId client
   * @param attachmentId document
   * @param request HTTP request (client address of the link audit)
   * @return redirect to the file, or the file
   */
  @GetMapping("/documents/{attachmentId}/content")
  @PreAuthorize("hasAuthority('CSF_VIEW') and hasAuthority('ATTACHMENT_VIEW')")
  public ResponseEntity<byte[]> download(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @PathVariable Long attachmentId,
      HttpServletRequest request) {
    return downloads.respond(documents.download(companyId, clientId, attachmentId), request);
  }

  /**
   * Downloads documents of the client's records as one ZIP file (logged).
   *
   * @param companyId company
   * @param clientId client
   * @param ids documents
   * @param name file name without extension
   * @return ZIP file
   */
  @GetMapping("/documents/zip")
  @PreAuthorize("hasAuthority('CSF_VIEW') and hasAuthority('ATTACHMENT_VIEW')")
  public ResponseEntity<byte[]> zip(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @RequestParam List<Long> ids,
      @RequestParam(defaultValue = "documents") String name) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/zip"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(name + ".zip"))
        .body(documents.zip(companyId, clientId, ids));
  }
}
