package com.iortatechnxt.brokerverse.crm.api;

import com.iortatechnxt.brokerverse.crm.api.dto.ClientActionRequest;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientResponse;
import com.iortatechnxt.brokerverse.crm.api.dto.KycChecklistResponse;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientCompleteness;
import com.iortatechnxt.brokerverse.crm.service.ClientOnboardingService;
import com.iortatechnxt.brokerverse.crm.service.KycDocumentService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Client onboarding (BRNB.090/101): KYC documents and checklist, submission, verification by a
 * checker, confirmation and deactivation.
 */
@RestController
@RequestMapping("/api/v1/crm/clients/{id}")
public class ClientOnboardingController {

  private static final String MAINTAIN = "hasAuthority('CLIENT_MAINTAIN')";

  private final ClientOnboardingService onboarding;
  private final KycDocumentService kyc;
  private final ClientCompleteness completeness;

  /**
   * Creates the controller.
   *
   * @param onboarding onboarding steps
   * @param kyc KYC documents
   * @param completeness completeness rules
   */
  public ClientOnboardingController(
      ClientOnboardingService onboarding, KycDocumentService kyc, ClientCompleteness completeness) {
    this.onboarding = onboarding;
    this.kyc = kyc;
    this.completeness = completeness;
  }

  /**
   * KYC checklist: mandatory documents of the client type and the uploads.
   *
   * @param id client
   * @return checklist
   */
  @GetMapping("/kyc-checklist")
  @PreAuthorize("hasAuthority('CLIENT_VIEW')")
  public KycChecklistResponse checklist(@PathVariable Long id) {
    return KycChecklistResponse.from(kyc.checklist(id));
  }

  /**
   * Uploads a KYC document of a type.
   *
   * @param id client
   * @param documentType document type (list of values DOCUMENT_TYPE)
   * @param file file
   * @return checklist after the upload
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/kyc-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(MAINTAIN)
  public KycChecklistResponse upload(
      @PathVariable Long id,
      @RequestParam String documentType,
      @RequestParam("file") MultipartFile file)
      throws IOException {
    return KycChecklistResponse.from(
        kyc.upload(id, documentType, file.getOriginalFilename(), file.getBytes()));
  }

  /**
   * Submits the KYC for verification.
   *
   * @param id client
   * @param body comment
   * @return client
   */
  @PostMapping("/submit-kyc")
  @PreAuthorize(MAINTAIN)
  public ClientResponse submitKyc(
      @PathVariable Long id, @Valid @RequestBody ClientActionRequest body) {
    return response(onboarding.submitKyc(id, body.cleanComment()));
  }

  /**
   * Verifies the KYC (checker, not the maker); also records the periodic review.
   *
   * @param id client
   * @param body comment
   * @return client
   */
  @PostMapping("/verify-kyc")
  @PreAuthorize("hasAuthority('CLIENT_APPROVE')")
  public ClientResponse verifyKyc(
      @PathVariable Long id, @Valid @RequestBody ClientActionRequest body) {
    return response(onboarding.verifyKyc(id, body.cleanComment()));
  }

  /**
   * Confirms the client: client code, sub-ledger party.
   *
   * @param id client
   * @param body comment
   * @return confirmed client
   */
  @PostMapping("/confirm")
  @PreAuthorize("hasAnyAuthority('CLIENT_MAINTAIN', 'CLIENT_APPROVE')")
  public ClientResponse confirm(
      @PathVariable Long id, @Valid @RequestBody ClientActionRequest body) {
    return response(onboarding.confirm(id, body.cleanComment()));
  }

  /**
   * Deactivates the client with a reason.
   *
   * @param id client
   * @param body reason and comment
   * @return client
   */
  @PostMapping("/deactivate")
  @PreAuthorize("hasAnyAuthority('CLIENT_MAINTAIN', 'CLIENT_APPROVE')")
  public ClientResponse deactivate(
      @PathVariable Long id, @Valid @RequestBody ClientActionRequest body) {
    return response(onboarding.deactivate(id, body.reasonCode(), body.cleanComment()));
  }

  private ClientResponse response(Client c) {
    return ClientResponse.from(c, completeness.missing(c));
  }
}
