package com.iortatechnxt.brokerverse.renewal.upload.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The upload outcome summaries of the Renewal uploads (FRRN.012.03, 012.04, 012.08, 012.09, the
 * insurer disposition and update uploads, FRRN.015.05, 015.06): one row per uploaded file with its
 * Upload ID, file name, date and time, status (Successful, Failed, Partially Successful) and
 * remarks, latest first, searchable by file name and filterable by status; the record-level view of
 * an upload with the matching and processing status and the reason of each record; and the
 * Processing Result workbook named after the upload date.
 */
@Service
@Transactional(readOnly = true)
public class RenewalUploadCentre {

  /** Upload status of a file whose every record was processed. */
  public static final String SUCCESSFUL = "Successful";

  /** Upload status of a file of which no record was processed. */
  public static final String FAILED = "Failed";

  /** Upload status of a file of which some records were processed. */
  public static final String PARTIAL = "Partially Successful";

  private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("MMddyyyy");
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final TypeReference<Map<String, String>> ROW = new TypeReference<>() {};

  private static final String JOBS =
      "select job_no, handler_code, file_name, created_at, created_by, status, total_rows,"
          + " invalid_rows, committed_rows, failed_rows from bulk_job where company_id = :companyId"
          + " and handler_code in (:handlers) and status <> 'CANCELLED'"
          + " and (cast(:search as varchar) is null or file_name ilike '%' || :search || '%')"
          + " order by created_at desc, id desc limit 500";

  private static final String ROWS =
      "select r.row_no, r.data, r.status, r.messages, r.result_ref, r.outcome from bulk_row r"
          + " join bulk_job j on j.id = r.job_id where j.company_id = :companyId"
          + " and j.job_no = :jobNo order by r.row_no";

  private final NamedParameterJdbcTemplate jdbc;
  private final DocumentComposer composer;
  private final ObjectMapper json;

  /**
   * Creates the centre.
   *
   * @param jdbc uploads and their rows
   * @param composer Processing Result workbook
   * @param json values of a row
   */
  public RenewalUploadCentre(
      NamedParameterJdbcTemplate jdbc, DocumentComposer composer, ObjectMapper json) {
    this.jdbc = jdbc;
    this.composer = composer;
    this.json = json;
  }

  /**
   * The uploads of a kind, latest first.
   *
   * @param companyId company
   * @param kind kind of upload
   * @param search part of the file name, may be null
   * @param status Successful, Failed or Partially Successful, may be null
   * @return uploads
   */
  public List<Upload> uploads(Long companyId, UploadKind kind, String search, String status) {
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", companyId);
    args.put("handlers", kind.handlers());
    args.put("search", search == null || search.isBlank() ? null : search.strip());
    List<Upload> all =
        jdbc.query(
            JOBS,
            args,
            (rs, i) -> {
              int total = rs.getInt("total_rows");
              int done = rs.getInt("committed_rows");
              int failed = rs.getInt("failed_rows") + rs.getInt("invalid_rows");
              boolean pending = "VALIDATED".equals(rs.getString("status"));
              return new Upload(
                  rs.getString("job_no"),
                  rs.getString("handler_code"),
                  rs.getString("file_name"),
                  rs.getTimestamp("created_at").toInstant(),
                  rs.getString("created_by"),
                  pending ? "Pending" : status(done, failed),
                  remarks(pending, total, done, failed),
                  total,
                  done,
                  failed);
            });
    return status == null || status.isBlank()
        ? all
        : all.stream().filter(u -> u.status().equals(status.strip())).toList();
  }

  /**
   * The record-level results of an upload.
   *
   * @param companyId company
   * @param jobNo Upload ID
   * @param matching MATCHED or UNMATCHED, may be null
   * @param search text in any value of the record, may be null
   * @return records
   */
  public List<Record> records(Long companyId, String jobNo, String matching, String search) {
    List<Record> rows =
        jdbc.query(
            ROWS,
            Map.of("companyId", companyId, "jobNo", jobNo),
            (rs, i) ->
                new Record(
                    rs.getInt("row_no"),
                    values(rs.getString("data")),
                    rs.getString("outcome"),
                    processing(rs.getString("status")),
                    reason(rs.getString("messages"), rs.getString("result_ref"))));
    String text = search == null || search.isBlank() ? null : search.strip();
    return rows.stream()
        .filter(r -> matching == null || matching.isBlank() || matching.equals(r.matchingStatus()))
        .filter(r -> text == null || r.values().values().stream().anyMatch(v -> contains(v, text)))
        .toList();
  }

  /**
   * The Processing Result workbook of an upload: the values of each record with its statuses and
   * reason, named "&lt;kind&gt; Processing Result_ MMDDYYYY.xlsx" after the upload date.
   *
   * @param companyId company
   * @param kind kind of upload
   * @param jobNo Upload ID
   * @return the workbook
   */
  public MessageFile result(Long companyId, UploadKind kind, String jobNo) {
    Upload upload =
        uploads(companyId, kind, null, null).stream()
            .filter(u -> u.uploadId().equals(jobNo))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Upload", jobNo));
    List<Record> records = records(companyId, jobNo, null, null);
    List<String> headers = new ArrayList<>();
    records.stream()
        .flatMap(r -> r.values().keySet().stream())
        .filter(h -> !headers.contains(h))
        .forEach(headers::add);
    List<String> all = new ArrayList<>(headers);
    all.addAll(List.of("Matching Status", "Processing Status", "Reason"));
    List<List<Object>> rows = new ArrayList<>();
    for (Record r : records) {
      List<Object> row = new ArrayList<>();
      headers.forEach(h -> row.add(r.values().get(h)));
      row.add(matchingLabel(r.matchingStatus()));
      row.add(r.processingStatus());
      row.add(r.reason());
      rows.add(row);
    }
    String name =
        kind.resultPrefix()
            + " Processing Result_ "
            + FILE_DATE.format(BusinessClock.dateOf(upload.uploadedAt()))
            + ".xlsx";
    return new MessageFile(
        name, XLSX, composer.xlsx(new SheetSpec("Processing Result", all, rows)));
  }

  private Map<String, String> values(String data) {
    if (data == null || data.isBlank()) {
      return Map.of();
    }
    try {
      return new LinkedHashMap<>(json.readValue(data, ROW));
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }

  static String status(int done, int failed) {
    if (failed == 0 && done > 0) {
      return SUCCESSFUL;
    }
    return done == 0 ? FAILED : PARTIAL;
  }

  private static String remarks(boolean pending, int total, int done, int failed) {
    if (pending) {
      return "Validated, not processed yet";
    }
    if (failed == 0) {
      return done + " of " + total + " records processed";
    }
    return failed + " of " + total + " records not processed: see the record-level results";
  }

  private static String processing(String rowStatus) {
    return "COMMITTED".equals(rowStatus) ? "Success" : "Failed";
  }

  private static String reason(String messages, String result) {
    return messages != null && !messages.isBlank() ? messages : result;
  }

  private static String matchingLabel(String matching) {
    if (matching == null) {
      return "";
    }
    return "MATCHED".equals(matching) ? "Matched" : "Unmatched";
  }

  private static boolean contains(String value, String text) {
    return value != null
        && java.util.regex.Pattern.compile(
                java.util.regex.Pattern.quote(text), java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(value)
            .find();
  }

  /**
   * An uploaded file.
   *
   * @param uploadId Upload ID
   * @param handler upload type
   * @param fileName file name
   * @param uploadedAt upload date and time
   * @param uploadedBy uploading user
   * @param status Successful, Failed, Partially Successful or Pending
   * @param remarks reason or remarks
   * @param total records in the file
   * @param processed records processed
   * @param failed records not processed
   */
  public record Upload(
      String uploadId,
      String handler,
      String fileName,
      Instant uploadedAt,
      String uploadedBy,
      String status,
      String remarks,
      int total,
      int processed,
      int failed) {}

  /**
   * A record of an upload.
   *
   * @param rowNo row of the file
   * @param values values of the record by column
   * @param matchingStatus MATCHED, UNMATCHED or null
   * @param processingStatus Success or Failed
   * @param reason reason, or the outcome of the record
   */
  public record Record(
      int rowNo,
      Map<String, String> values,
      String matchingStatus,
      String processingStatus,
      String reason) {}
}
