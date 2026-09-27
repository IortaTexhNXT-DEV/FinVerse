package com.iortatechnxt.brokerverse.submitted.masterlist.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmExtraction;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.ExtractionDtos.ExtractionView;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.PolicyDetail;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.PolicyRequest;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.ExtractionService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.ExtractionService.DocumentUpload;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.ProposalMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Extraction Review (FRS FR-SP-002): upload of a policy document for a record or a new record, the
 * proposals waiting for confirmation, and confirm or reject.
 */
@RestController
@RequestMapping("/api/v1/submitted/extractions")
public class ExtractionController {

  private final ExtractionService extractions;
  private final MasterlistService masterlist;

  /**
   * Creates the controller.
   *
   * @param extractions extractions
   * @param masterlist masterlist
   */
  public ExtractionController(ExtractionService extractions, MasterlistService masterlist) {
    this.extractions = extractions;
    this.masterlist = masterlist;
  }

  /**
   * Uploads a policy document and proposes its fields.
   *
   * @param companyId company
   * @param policyId record, empty for a new record
   * @param segment segment of a new record
   * @param businessType business type of a new record
   * @param file document
   * @return the extraction
   * @throws IOException when the file cannot be read
   */
  @PostMapping
  @PreAuthorize(MasterlistController.MAINTAIN)
  @Transactional
  public ExtractionView upload(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long policyId,
      @RequestParam(required = false) String segment,
      @RequestParam(required = false) SbmBusinessType businessType,
      @RequestParam MultipartFile file)
      throws IOException {
    SbmExtraction x =
        extractions.upload(
            new DocumentUpload(
                companyId,
                policyId,
                segment,
                businessType == null ? SbmBusinessType.NB : businessType,
                file.getOriginalFilename(),
                file.getBytes()));
    return view(x);
  }

  /**
   * Extractions of a status tab: to confirm (proposed and not readable) or decided.
   *
   * @param companyId company
   * @param decided false for the proposals to confirm, true for the decided ones
   * @param pageable page
   * @return extractions
   */
  @GetMapping
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public PageResponse<ExtractionView> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "false") boolean decided,
      Pageable pageable) {
    List<SbmExtraction.Status> statuses =
        decided
            ? List.of(SbmExtraction.Status.CONFIRMED, SbmExtraction.Status.REJECTED)
            : List.of(SbmExtraction.Status.PROPOSED, SbmExtraction.Status.FAILED);
    return PageResponse.of(extractions.list(companyId, statuses, pageable), this::view);
  }

  /**
   * An extraction with the data to confirm.
   *
   * @param id extraction
   * @return extraction
   */
  @GetMapping("/{id}")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public ExtractionView get(@PathVariable Long id) {
    return view(extractions.get(id));
  }

  /**
   * Extractions of a record.
   *
   * @param policyId record
   * @return extractions
   */
  @GetMapping("/of-policy/{policyId}")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public List<ExtractionView> ofPolicy(@PathVariable Long policyId) {
    masterlist.get(policyId);
    return extractions.ofPolicy(policyId).stream().map(this::view).toList();
  }

  /**
   * Confirms the values.
   *
   * @param id extraction
   * @param request values as confirmed
   * @return the record
   */
  @PostMapping("/{id}/confirm")
  @PreAuthorize(MasterlistController.MAINTAIN)
  @Transactional
  public PolicyDetail confirm(@PathVariable Long id, @Valid @RequestBody PolicyRequest request) {
    return PolicyDetail.from(extractions.confirm(id, request.data()));
  }

  /**
   * Rejects the proposal.
   *
   * @param id extraction
   * @param request reason
   * @return the extraction
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(MasterlistController.MAINTAIN)
  @Transactional
  public ExtractionView reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return view(extractions.reject(id, request.reason()));
  }

  private ExtractionView view(SbmExtraction x) {
    SbmPolicyData current =
        x.getPolicyId() == null ? null : masterlist.require(x.getPolicyId()).data();
    return ExtractionView.from(x, ProposalMapper.proposed(x, current), current);
  }
}
