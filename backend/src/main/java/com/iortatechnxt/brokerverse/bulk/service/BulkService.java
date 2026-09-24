package com.iortatechnxt.brokerverse.bulk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobRepository;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRepository;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs bulk uploads: read and check the file against the handler's template, sanitise and validate
 * every row (framework checks, in-file duplicates, handler rules), keep the result for review, then
 * commit the valid rows one transaction each so a failing row never blocks the others
 * (BRNB.024/025/039).
 */
@Service
public class BulkService {

  private static final String ENTITY = "BulkJob";
  private static final int DEFAULT_MAX_ROWS = 5000;
  private static final TypeReference<Map<String, String>> ROW_TYPE = new TypeReference<>() {};

  private final BulkHandlerRegistry registry;
  private final BulkFileReader reader;
  private final BulkJobRepository jobs;
  private final BulkRowRepository rows;
  private final DocumentNumberService numbers;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final ObjectMapper json;
  private final TransactionTemplate tx;
  private final TransactionTemplate rowTx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param registry handlers
   * @param reader file reader
   * @param jobs jobs
   * @param rows rows
   * @param numbers document numbers
   * @param parameters business parameters (maximum rows)
   * @param audit audit trail
   * @param json JSON mapper (row storage)
   * @param txManager transaction manager
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BulkService(
      BulkHandlerRegistry registry,
      BulkFileReader reader,
      BulkJobRepository jobs,
      BulkRowRepository rows,
      DocumentNumberService numbers,
      SystemParameterService parameters,
      AuditTrailService audit,
      ObjectMapper json,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.registry = registry;
    this.reader = reader;
    this.jobs = jobs;
    this.rows = rows;
    this.numbers = numbers;
    this.parameters = parameters;
    this.audit = audit;
    this.json = json;
    this.tx = new TransactionTemplate(txManager);
    this.rowTx = new TransactionTemplate(txManager);
    this.rowTx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * The template of a handler.
   *
   * @param handlerCode handler
   * @return xlsx bytes
   */
  public byte[] template(String handlerCode) {
    return BulkWorkbooks.template(registry.require(handlerCode));
  }

  /**
   * Uploads and validates a file; nothing is created until {@link #commit}.
   *
   * @param upload company, handler, file and parameters
   * @return the validated job
   */
  public BulkJob upload(BulkUpload upload) {
    BulkImportHandler handler = registry.require(upload.handlerCode());
    ParsedFile file = reader.read(upload.fileName(), upload.content());
    requireTemplateHeaders(handler, file);
    int max = parameters.intValue("BULK_MAX_ROWS", DEFAULT_MAX_ROWS);
    if (file.rows().isEmpty()) {
      throw new BusinessRuleException("BULK_FILE_EMPTY", "The file has no data rows");
    }
    if (file.rows().size() > max) {
      throw new BusinessRuleException(
          "BULK_FILE_TOO_LARGE",
          "The file has " + file.rows().size() + " rows; the maximum is " + max);
    }
    return tx.execute(
        s -> {
          String jobNo = numbers.next("BLK-" + LocalDate.now(clock).getYear());
          BulkContext context =
              new BulkContext(upload.companyId(), jobNo, LocalDate.now(clock), upload.parameters());
          return validateAndStore(handler, upload, file, context);
        });
  }

  private BulkJob validateAndStore(
      BulkImportHandler handler, BulkUpload upload, ParsedFile file, BulkContext context) {
    BulkJob job =
        jobs.save(
            new BulkJob(
                upload.companyId(),
                context.jobNo(),
                handler.code(),
                upload.fileName(),
                write(upload.parameters())));
    Map<String, Integer> seen = new HashMap<>();
    int valid = 0;
    for (RawRow raw : file.rows()) {
      BulkRow row = BulkRowValidator.sanitize(handler, raw);
      List<String> errors = new ArrayList<>(BulkRowValidator.check(handler, row));
      if (errors.isEmpty()) {
        String key = handler.duplicateKey(row);
        Integer first = key == null ? null : seen.putIfAbsent(key, row.rowNo());
        if (first != null) {
          errors.add("Duplicate of row " + first + " in this file");
        } else {
          errors.addAll(handler.validate(row, context));
        }
      }
      rows.save(new BulkRowRecord(job.getId(), row.rowNo(), write(row.values()), errors));
      if (errors.isEmpty()) {
        valid++;
      }
    }
    job.validated(valid, file.rows().size() - valid);
    audit.record(
        ENTITY,
        job.getJobNo(),
        AuditAction.CREATE,
        handler.title()
            + ": "
            + upload.fileName()
            + " - "
            + valid
            + " valid of "
            + file.rows().size());
    return job;
  }

  private static void requireTemplateHeaders(BulkImportHandler handler, ParsedFile file) {
    List<String> missing =
        handler.columns().stream()
            .map(BulkColumn::header)
            .filter(h -> !file.headers().contains(h))
            .toList();
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "BULK_TEMPLATE_MISMATCH",
          "The file does not follow the current template; missing column(s): "
              + String.join(", ", missing));
    }
  }

  /**
   * Commits the valid rows, one transaction per row.
   *
   * @param jobId job
   * @return the completed job
   */
  public BulkJob commit(Long jobId) {
    BulkJob job = requireOpen(jobId);
    BulkImportHandler handler = registry.require(job.getHandlerCode());
    BulkContext context =
        new BulkContext(
            job.getCompanyId(), job.getJobNo(), LocalDate.now(clock), read(job.getParameters()));
    List<BulkRowRecord> valid = rows.findByJobIdAndStatusOrderByRowNo(jobId, BulkRowStatus.VALID);
    int committed = 0;
    int failed = 0;
    for (BulkRowRecord record : valid) {
      if (commitRow(handler, record, context)) {
        committed++;
      } else {
        failed++;
      }
    }
    int ok = committed;
    int ko = failed;
    return tx.execute(
        s -> {
          BulkJob j = requireOpen(jobId);
          j.completed(ok, ko, clock.instant());
          audit.record(
              ENTITY,
              j.getJobNo(),
              AuditAction.UPDATE,
              "Committed " + ok + " row(s), " + ko + " failed");
          return j;
        });
  }

  private boolean commitRow(BulkImportHandler handler, BulkRowRecord record, BulkContext context) {
    BulkRow row = new BulkRow(record.getRowNo(), read(record.getData()));
    try {
      String reference = rowTx.execute(s -> handler.commit(row, context));
      tx.executeWithoutResult(
          s -> rows.findById(record.getId()).ifPresent(r -> r.committed(reference)));
      return true;
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      tx.executeWithoutResult(
          s -> rows.findById(record.getId()).ifPresent(r -> r.failed(e.getMessage())));
      return false;
    }
  }

  /**
   * Discards a validated job.
   *
   * @param jobId job
   * @return the job
   */
  @Transactional
  public BulkJob cancel(Long jobId) {
    BulkJob job = requireOpen(jobId);
    registry.require(job.getHandlerCode());
    job.cancel(clock.instant());
    audit.record(ENTITY, job.getJobNo(), AuditAction.UPDATE, "Cancelled");
    return job;
  }

  /**
   * Jobs of a company, optionally for one handler, newest first.
   *
   * @param companyId company
   * @param handlerCode handler, null for all
   * @param pageable page
   * @return jobs
   */
  @Transactional(readOnly = true)
  public Page<BulkJob> jobs(Long companyId, String handlerCode, Pageable pageable) {
    return handlerCode == null
        ? jobs.findByCompanyIdOrderByIdDesc(companyId, pageable)
        : jobs.findByCompanyIdAndHandlerCodeOrderByIdDesc(companyId, handlerCode, pageable);
  }

  /**
   * One job.
   *
   * @param jobId job
   * @return job
   */
  @Transactional(readOnly = true)
  public BulkJob job(Long jobId) {
    return jobs.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(ENTITY, jobId));
  }

  /**
   * Rows of a job.
   *
   * @param jobId job
   * @param status status filter, null for all
   * @param pageable page
   * @return rows
   */
  @Transactional(readOnly = true)
  public Page<BulkRowRecord> rows(Long jobId, BulkRowStatus status, Pageable pageable) {
    return status == null
        ? rows.findByJobIdOrderByRowNo(jobId, pageable)
        : rows.findByJobIdAndStatusOrderByRowNo(jobId, status, pageable);
  }

  /**
   * Values of a stored row.
   *
   * @param row row
   * @return values by header
   */
  public Map<String, String> values(BulkRowRecord row) {
    return read(row.getData());
  }

  /**
   * The result report (summary and every row with status and messages).
   *
   * @param jobId job
   * @return xlsx bytes
   */
  @Transactional(readOnly = true)
  public byte[] report(Long jobId) {
    BulkJob job = job(jobId);
    List<BulkRowRecord> all = rows.findByJobIdOrderByRowNo(jobId);
    Map<Long, Map<String, String>> values = new HashMap<>();
    all.forEach(r -> values.put(r.getId(), read(r.getData())));
    return BulkWorkbooks.report(job, registry.get(job.getHandlerCode()).columns(), all, values);
  }

  private BulkJob requireOpen(Long jobId) {
    BulkJob job = job(jobId);
    job.requireValidated();
    return job;
  }

  private String write(Map<String, String> values) {
    if (values == null || values.isEmpty()) {
      return values == null ? null : "{}";
    }
    try {
      return json.writeValueAsString(values);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private Map<String, String> read(String data) {
    if (data == null || data.isBlank()) {
      return Map.of();
    }
    try {
      return json.readValue(data, ROW_TYPE);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
