package com.iortatechnxt.brokerverse.renewal.upload.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.renewal.upload.service.RenewalUploadCentre;
import com.iortatechnxt.brokerverse.renewal.upload.service.UploadKind;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Upload outcome summaries and record-level results of the Renewal uploads. */
@RestController
@RequestMapping("/api/v1/renewal/uploads")
@PreAuthorize(
    "hasAnyAuthority('RNW_LAMD_UPLOAD','RNW_INSURER','RNW_PROCESS','RNW_DISPOSE','RNW_REVIEW',"
        + "'RNW_REPORT_VIEW')")
public class RenewalUploadController {

  private final RenewalUploadCentre centre;

  /**
   * Creates the controller.
   *
   * @param centre upload summaries
   */
  public RenewalUploadController(RenewalUploadCentre centre) {
    this.centre = centre;
  }

  /**
   * The uploads of a kind, latest first.
   *
   * @param companyId company
   * @param kind kind of upload
   * @param search part of the file name
   * @param status Successful, Failed or Partially Successful
   * @return uploads
   */
  @GetMapping
  public List<RenewalUploadCentre.Upload> uploads(
      @RequestParam Long companyId,
      @RequestParam UploadKind kind,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status) {
    return centre.uploads(companyId, kind, search, status);
  }

  /**
   * The record-level results of an upload.
   *
   * @param companyId company
   * @param jobNo Upload ID
   * @param matching MATCHED or UNMATCHED
   * @param search text
   * @return records
   */
  @GetMapping("/{jobNo}/records")
  public List<RenewalUploadCentre.Record> records(
      @RequestParam Long companyId,
      @PathVariable String jobNo,
      @RequestParam(required = false) String matching,
      @RequestParam(required = false) String search) {
    return centre.records(companyId, jobNo, matching, search);
  }

  /**
   * The Processing Result workbook of an upload.
   *
   * @param companyId company
   * @param kind kind of upload
   * @param jobNo Upload ID
   * @return the workbook
   */
  @GetMapping("/{jobNo}/result")
  public ResponseEntity<byte[]> result(
      @RequestParam Long companyId, @RequestParam UploadKind kind, @PathVariable String jobNo) {
    MessageFile f = centre.result(companyId, kind, jobNo);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(f.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(f.fileName()))
        .body(f.content());
  }
}
