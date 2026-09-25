package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionError;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionErrorRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRunRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSourceRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The digest of records that failed ingestion (SNSRP-202; FR-SS-021): the records not yet sent are
 * e-mailed with their source, run, line, reason and raw record to the addresses in {@code
 * SCR_INGEST_ALERT_RECIPIENTS}, then stamped. No failed record means no e-mail; no valid recipient
 * means the digest is skipped with an alert "no recipients".
 */
@Service
@Transactional
public class IngestErrorDigest {

  /** Parameter holding the recipients (V1050). */
  static final String RECIPIENTS = "SCR_INGEST_ALERT_RECIPIENTS";

  private static final Pattern EMAIL = Pattern.compile("[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;]+");
  private static final String PURPOSE = "SCR_INGEST_DIGEST";

  private final IngestionErrorRepository errors;
  private final IngestionRunRepository runs;
  private final WatchlistSourceRepository sources;
  private final SystemParameterService parameters;
  private final MessageService messages;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the digest.
   *
   * @param errors failed records
   * @param runs runs
   * @param sources sources
   * @param parameters business parameters
   * @param messages e-mail queue
   * @param alerts alerts
   * @param audit audit trail
   * @param clock clock
   */
  public IngestErrorDigest(
      IngestionErrorRepository errors,
      IngestionRunRepository runs,
      WatchlistSourceRepository sources,
      SystemParameterService parameters,
      MessageService messages,
      AlertService alerts,
      AuditTrailService audit,
      Clock clock) {
    this.errors = errors;
    this.runs = runs;
    this.sources = sources;
    this.parameters = parameters;
    this.messages = messages;
    this.alerts = alerts;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Sends the digest of the failed records not yet sent.
   *
   * @return the outcome (records sent)
   */
  public JobOutcome send() {
    List<IngestionError> pending = errors.findByDigestedAtIsNullOrderByRunIdAscLineNoAsc();
    if (pending.isEmpty()) {
      return new JobOutcome(0, "No failed watchlist record since the last digest");
    }
    List<String> to = recipients(parameters.text(RECIPIENTS, ""));
    if (to.isEmpty()) {
      String message =
          "Digest of "
              + pending.size()
              + " failed watchlist record(s) skipped: no recipients in "
              + RECIPIENTS;
      alerts.raise(
          WatchlistIngestionService.ALERT,
          new AlertFacts(
              null,
              null,
              WatchlistIngestionService.RUN_ENTITY,
              "DIGEST",
              message,
              null,
              "SCR_INGEST_DIGEST_NO_RECIPIENTS"));
      return new JobOutcome(0, message);
    }
    Digest digest = compose(pending);
    Instant now = clock.instant();
    messages.queueEmail(
        new OutboundEmail(
            null,
            PURPOSE,
            to,
            List.of(),
            "Watchlist ingestion errors: " + pending.size() + " record(s)",
            digest.body(),
            List.of(
                new MessageFile(
                    "watchlist-ingestion-errors.csv",
                    "text/csv",
                    digest.csv().getBytes(StandardCharsets.UTF_8))),
            null,
            new RecordLink(WatchlistIngestionService.RUN_ENTITY, "DIGEST", "Ingestion errors")));
    pending.forEach(e -> e.digested(now));
    audit.record(
        WatchlistIngestionService.RUN_ENTITY,
        "DIGEST",
        AuditAction.EXPORT,
        "Ingestion error digest of " + pending.size() + " record(s) to " + String.join(", ", to));
    return new JobOutcome(
        pending.size(), pending.size() + " failed record(s) sent to " + to.size());
  }

  private Digest compose(List<IngestionError> pending) {
    Map<Long, IngestionRun> runById =
        runs
            .findAllById(pending.stream().map(IngestionError::getRunId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(IngestionRun::getId, Function.identity()));
    Map<Long, String> sourceCode =
        sources.findAll().stream()
            .collect(Collectors.toMap(WatchlistSource::getId, WatchlistSource::getCode));
    StringBuilder body = new StringBuilder(256);
    StringBuilder csv = new StringBuilder("Source,Run,Line,Reason,Record\n");
    body.append("Records that failed watchlist ingestion since the last digest (SNSRP-202):\n\n");
    for (IngestionError e : pending) {
      IngestionRun run = runById.get(e.getRunId());
      String source = run == null ? "" : sourceCode.getOrDefault(run.getSourceId(), "");
      String runNo = run == null ? "" : run.getRunNo();
      body.append(source)
          .append(" | ")
          .append(runNo)
          .append(" | line ")
          .append(e.getLineNo())
          .append(" | ")
          .append(e.getReason())
          .append('\n');
      csv.append(cell(source))
          .append(',')
          .append(cell(runNo))
          .append(',')
          .append(e.getLineNo())
          .append(',')
          .append(cell(e.getReason()))
          .append(',')
          .append(cell(e.getRawRecord()))
          .append('\n');
    }
    return new Digest(body.toString(), csv.toString());
  }

  /** E-mail body and CSV attachment of a digest. */
  private record Digest(String body, String csv) {}

  /**
   * The valid addresses of the parameter (comma or semicolon separated).
   *
   * @param text parameter value
   * @return addresses
   */
  static List<String> recipients(String text) {
    return Arrays.stream(text == null ? new String[0] : text.split("[,;]"))
        .map(String::trim)
        .filter(s -> EMAIL.matcher(s).matches())
        .distinct()
        .toList();
  }

  private static String cell(String value) {
    String v = value == null ? "" : value;
    return "\"" + v.replace("\"", "\"\"") + "\"";
  }
}
