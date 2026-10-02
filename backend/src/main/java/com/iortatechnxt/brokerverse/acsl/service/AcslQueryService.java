package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCase;
import com.iortatechnxt.brokerverse.acsl.domain.AcslCaseRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CaseStage;
import com.iortatechnxt.brokerverse.acsl.domain.CaseType;
import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionStage;
import com.iortatechnxt.brokerverse.acsl.domain.ReconBucket;
import com.iortatechnxt.brokerverse.acsl.domain.ReconResult;
import com.iortatechnxt.brokerverse.acsl.domain.ReconResultRepository;
import com.iortatechnxt.brokerverse.acsl.domain.ReconRun;
import com.iortatechnxt.brokerverse.acsl.domain.ReconRunRepository;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLine;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLineRepository;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUpload;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUploadRepository;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of ACSL (ACSL 2.3.x, 2.4.0, 2.5.x, 2.14.1): the cases board, the corrections, the SOA
 * uploads with their upload log and reconciliation results.
 */
@Service
@Transactional(readOnly = true)
public class AcslQueryService {

  private final AcslCaseRepository cases;
  private final CorrectionRepository corrections;
  private final SoaUploadRepository uploads;
  private final SoaLineRepository lines;
  private final ReconRunRepository runs;
  private final ReconResultRepository results;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param corrections corrections
   * @param uploads SOA uploads
   * @param lines SOA lines
   * @param runs reconciliation runs
   * @param results reconciliation results
   */
  public AcslQueryService(
      AcslCaseRepository cases,
      CorrectionRepository corrections,
      SoaUploadRepository uploads,
      SoaLineRepository lines,
      ReconRunRepository runs,
      ReconResultRepository results) {
    this.cases = cases;
    this.corrections = corrections;
    this.uploads = uploads;
    this.lines = lines;
    this.runs = runs;
    this.results = results;
  }

  /**
   * Cases board search.
   *
   * @param companyId company
   * @param stage stage or null
   * @param type type or null
   * @param text case, subject, invoice or AR
   * @param pageable page
   * @return cases
   */
  public Page<AcslCase> cases(
      Long companyId, CaseStage stage, CaseType type, String text, Pageable pageable) {
    return cases.search(companyId, stage, type, like(text), pageable);
  }

  /**
   * Cases per stage.
   *
   * @param companyId company
   * @return count per stage
   */
  public Map<CaseStage, Long> caseCounts(Long companyId) {
    Map<CaseStage, Long> counts = new EnumMap<>(CaseStage.class);
    cases.countByStage(companyId).forEach(r -> counts.put((CaseStage) r[0], (Long) r[1]));
    return counts;
  }

  /**
   * Cases of an invoice family.
   *
   * @param rootInvoiceNo root invoice
   * @return cases
   */
  public List<AcslCase> casesOfFamily(String rootInvoiceNo) {
    return cases.findByAccountRootInvoiceNoOrderByIdDesc(rootInvoiceNo);
  }

  /**
   * Corrections search.
   *
   * @param companyId company
   * @param stage stage or null
   * @param text correction, invoice or description
   * @param pageable page
   * @return corrections
   */
  public Page<Correction> corrections(
      Long companyId, CorrectionStage stage, String text, Pageable pageable) {
    return corrections.search(companyId, stage, like(text), pageable);
  }

  /**
   * Corrections per stage.
   *
   * @param companyId company
   * @return count per stage
   */
  public Map<CorrectionStage, Long> correctionCounts(Long companyId) {
    Map<CorrectionStage, Long> counts = new EnumMap<>(CorrectionStage.class);
    corrections
        .countByStage(companyId)
        .forEach(r -> counts.put((CorrectionStage) r[0], (Long) r[1]));
    return counts;
  }

  /**
   * SOA uploads.
   *
   * @param companyId company
   * @param text insurer or upload number
   * @param pageable page
   * @return uploads
   */
  public Page<SoaUpload> uploads(Long companyId, String text, Pageable pageable) {
    return uploads.search(companyId, like(text), pageable);
  }

  /**
   * One upload.
   *
   * @param id upload
   * @return upload
   */
  public SoaUpload upload(Long id) {
    return uploads
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(Acsl.SOA_ENTITY, id));
  }

  /**
   * One upload by number.
   *
   * @param uploadNo upload number
   * @return upload
   */
  public SoaUpload upload(String uploadNo) {
    return uploads
        .findByUploadNo(uploadNo)
        .orElseThrow(() -> new ResourceNotFoundException(Acsl.SOA_ENTITY, uploadNo));
  }

  /**
   * The upload log: every row with its status (ACSL 2.4.0).
   *
   * @param uploadId upload
   * @return rows in file order
   */
  public List<SoaLine> uploadLog(Long uploadId) {
    return lines.findByUploadIdOrderByRowNo(uploadId);
  }

  /**
   * The latest reconciliation run of an upload.
   *
   * @param upload upload
   * @return run
   */
  public Optional<ReconRun> lastRun(SoaUpload upload) {
    return upload.getLastRunId() == null ? Optional.empty() : runs.findById(upload.getLastRunId());
  }

  /**
   * Results of the latest run of an upload, optionally of one bucket.
   *
   * @param upload upload
   * @param bucket bucket or null
   * @return results in file order
   */
  public List<ReconResult> results(SoaUpload upload, ReconBucket bucket) {
    Long runId = upload.getLastRunId();
    if (runId == null) {
      return List.of();
    }
    return bucket == null
        ? results.findByRunIdOrderByRowNo(runId)
        : results.findByRunIdAndBucketOrderByRowNo(runId, bucket);
  }

  private static String like(String text) {
    return text == null || text.isBlank() ? "%" : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
  }
}
