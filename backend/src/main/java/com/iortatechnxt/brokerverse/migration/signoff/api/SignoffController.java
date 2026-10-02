package com.iortatechnxt.brokerverse.migration.signoff.api;

import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.SignRequest;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.SignoffResponse;
import com.iortatechnxt.brokerverse.migration.signoff.service.EvidenceService;
import com.iortatechnxt.brokerverse.migration.signoff.service.GateMatrix;
import com.iortatechnxt.brokerverse.migration.signoff.service.SignoffService;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Sign-off (FR-DM-022, FR-DM-023; screen Sign-off): the gate matrix of every object, the mapping
 * sign-off (G2), the list of sign-offs and their evidence files.
 */
@RestController
@RequestMapping("/api/v1/migration/signoffs")
public class SignoffController {

  private static final String VIEW = "hasAuthority('MIG_VIEW')";

  private final SignoffService signoffs;
  private final GateMatrix matrix;
  private final EvidenceService evidence;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param signoffs gates
   * @param matrix gate matrix
   * @param evidence evidence files
   * @param downloads file answers
   */
  public SignoffController(
      SignoffService signoffs,
      GateMatrix matrix,
      EvidenceService evidence,
      FileDownloads downloads) {
    this.signoffs = signoffs;
    this.matrix = matrix;
    this.evidence = evidence;
    this.downloads = downloads;
  }

  /**
   * The gate matrix.
   *
   * @param companyId company
   * @return one row per object
   */
  @GetMapping("/matrix")
  @PreAuthorize(VIEW)
  public List<GateMatrix.Row> matrix(@RequestParam Long companyId) {
    return matrix.matrix(companyId);
  }

  /**
   * All sign-offs of a company.
   *
   * @param companyId company
   * @return sign-offs, newest first
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<SignoffResponse> list(@RequestParam Long companyId) {
    return signoffs.all(companyId).stream().map(SignoffResponse::from).toList();
  }

  /**
   * Signs the mapping of an object (G2).
   *
   * @param companyId company
   * @param objectCode object
   * @param request decision
   * @return sign-off
   */
  @PostMapping("/mapping/{objectCode}")
  @PreAuthorize("hasAuthority('MIG_MAPPING_APPROVE')")
  public SignoffResponse signMapping(
      @RequestParam Long companyId,
      @PathVariable String objectCode,
      @RequestBody SignRequest request) {
    return SignoffResponse.from(
        signoffs.signMapping(companyId, objectCode, request.approve(), request.comment()));
  }

  /**
   * Attaches the evidence of a sign-off.
   *
   * @param signoffId sign-off
   * @param file evidence
   * @return sign-off
   * @throws IOException when the file cannot be read
   */
  @PostMapping("/{signoffId}/evidence")
  @PreAuthorize(
      "hasAnyAuthority('MIG_SIGNOFF', 'MIG_RECON_SIGNOFF', 'MIG_DQ_RESOLVE', 'MIG_MAPPING_APPROVE')")
  public SignoffResponse attach(
      @PathVariable Long signoffId, @RequestPart("file") MultipartFile file) throws IOException {
    return SignoffResponse.from(
        evidence.attach(signoffId, file.getOriginalFilename(), file.getBytes()));
  }

  /**
   * Downloads the evidence of a sign-off.
   *
   * @param signoffId sign-off
   * @param request HTTP request
   * @return file
   */
  @GetMapping("/{signoffId}/evidence")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> download(@PathVariable Long signoffId, HttpServletRequest request) {
    return downloads.respond(evidence.download(signoffId), request);
  }
}
