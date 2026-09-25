package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.SoaLayout;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLayoutRepository;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLine;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLineRepository;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUpload;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUploadRepository;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upload of an insurer statement of account (ACSL 2.2.1, 2.4.0): the file (.xlsx, .ods, .csv or
 * .txt) is read with the insurer's layout (the standard ACSL template until the insurer formats are
 * known, AQ21), every row is kept as loaded or failed with its reason (the upload log proving every
 * row was loaded), and the reconciliation runs at once (ACSL 2.13.0). The same file cannot be
 * uploaded twice for the insurer.
 */
@Service
@Transactional
public class SoaUploadService {

  /** Parameter: largest number of rows of an upload (V897). */
  public static final String MAX_ROWS = "ACSL_SOA_MAX_ROWS";

  private static final int DEFAULT_MAX_ROWS = 20_000;

  private final SoaUploadRepository uploads;
  private final SoaLineRepository lines;
  private final SoaLayoutRepository layouts;
  private final SoaRowParser parser;
  private final SoaReconciliation reconciliation;
  private final BulkFileReader reader;
  private final SystemParameterService parameters;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param uploads uploads
   * @param lines lines
   * @param layouts layouts
   * @param parser row parser
   * @param reconciliation reconciliation
   * @param reader file reader of the bulk framework
   * @param parameters parameters
   * @param numbers upload numbers
   * @param audit audit trail
   * @param clock clock
   */
  public SoaUploadService(
      SoaUploadRepository uploads,
      SoaLineRepository lines,
      SoaLayoutRepository layouts,
      SoaRowParser parser,
      SoaReconciliation reconciliation,
      BulkFileReader reader,
      SystemParameterService parameters,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.uploads = uploads;
    this.lines = lines;
    this.layouts = layouts;
    this.parser = parser;
    this.reconciliation = reconciliation;
    this.reader = reader;
    this.parameters = parameters;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Loads an insurer SOA and reconciles it.
   *
   * @param companyId company
   * @param period insurer and covered period
   * @param fileName file name
   * @param content file content
   * @return the upload with its counts and reconciliation run
   */
  public SoaUpload upload(
      Long companyId, SoaUpload.Period period, String fileName, byte[] content) {
    SoaUpload.Period checked = checked(period);
    String sha = Sha256.hex(content);
    uploads
        .findByCompanyIdAndInsurerCodeAndSha256(companyId, checked.insurerCode(), sha)
        .ifPresent(
            earlier -> {
              throw new BusinessRuleException(
                  "ACSL_SOA_DUPLICATE",
                  "This file was already uploaded for "
                      + checked.insurerCode()
                      + " as "
                      + earlier.getUploadNo());
            });
    SoaLayout layout = layoutOf(checked.insurerCode());
    ParsedFile file = reader.read(fileName, content);
    parser.requireHeaders(layout, file.headers());
    int max = parameters.intValue(MAX_ROWS, DEFAULT_MAX_ROWS);
    if (file.rows().size() > max) {
      throw new BusinessRuleException(
          "ACSL_SOA_TOO_LARGE", "An SOA upload has at most " + max + " rows");
    }
    SoaUpload upload =
        uploads.save(
            new SoaUpload(
                companyId,
                numbers.next("SOA-" + LocalDate.now(clock).getYear()),
                checked,
                new SoaUpload.FileFacts(fileName, sha),
                layout.getInsurerCode()));
    int loaded = 0;
    for (ParsedFile.RawRow row : file.rows()) {
      SoaLine line = lines.save(parser.parse(upload.getId(), layout, row));
      if (SoaLine.LOADED.equals(line.getStatus())) {
        loaded++;
      }
    }
    upload.counted(file.rows().size(), loaded, file.rows().size() - loaded);
    audit.record(
        Acsl.SOA_ENTITY,
        upload.getUploadNo(),
        AuditAction.CREATE,
        fileName + ": " + loaded + " of " + file.rows().size() + " row(s) loaded");
    reconciliation.run(upload);
    return upload;
  }

  /**
   * Reconciles an upload again (after the books changed).
   *
   * @param uploadId upload
   * @return the upload
   */
  public SoaUpload reconcile(Long uploadId) {
    SoaUpload upload =
        uploads
            .findById(uploadId)
            .orElseThrow(() -> new ResourceNotFoundException(Acsl.SOA_ENTITY, uploadId));
    reconciliation.run(upload);
    return upload;
  }

  /**
   * The layouts known, standard first.
   *
   * @return layouts
   */
  @Transactional(readOnly = true)
  public List<SoaLayout> layouts() {
    return layouts.findAllByOrderByInsurerCodeAsc();
  }

  private SoaLayout layoutOf(String insurerCode) {
    return layouts
        .findByInsurerCodeAndActiveTrue(insurerCode)
        .or(() -> layouts.findByInsurerCodeAndActiveTrue(SoaLayout.STANDARD))
        .orElseThrow(
            () -> new BusinessRuleException("ACSL_SOA_NO_LAYOUT", "No SOA layout is configured"));
  }

  private static SoaUpload.Period checked(SoaUpload.Period period) {
    String insurer = Acsl.blankToNull(period.insurerCode());
    if (insurer == null || period.from() == null || period.to() == null) {
      throw new BusinessRuleException(
          "ACSL_SOA_PERIOD", "Give the insurer and the covered period of the statement");
    }
    if (period.to().isBefore(period.from())) {
      throw new BusinessRuleException(
          "ACSL_SOA_PERIOD", "The covered period ends before it starts");
    }
    return new SoaUpload.Period(insurer, period.from(), period.to());
  }
}
