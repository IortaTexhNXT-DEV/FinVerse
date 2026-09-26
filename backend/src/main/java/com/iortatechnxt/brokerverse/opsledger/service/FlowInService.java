package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RecordStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RunStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInFeed;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInFeedRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRecord;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRecordRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun.FileRef;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRunRepository;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Flow-in framework (BRQID.004/005/006): every exchange with another system runs as a logged run of
 * a feed (timestamp, trigger, counts, status, errors); each record is processed once per
 * idempotency key in its own transaction, so failures are kept for review and re-sending without
 * stopping the run; a failed or partial run raises the {@code OPS_FLOW_IN_FAILED} alert and
 * notifies the interface administrators ({@code FLOWIN_MANAGE}).
 */
@Service
public class FlowInService {

  /** Alert and notification event of a failed run. */
  public static final String FAILED_ALERT = "OPS_FLOW_IN_FAILED";

  private static final String ENTITY = "FlowInRun";
  private static final int MAX_KEY = 120;

  private final FlowInFeedRepository feeds;
  private final FlowInRunRepository runs;
  private final FlowInRecordRepository records;
  private final Map<String, FlowInHandler> handlers;
  private final DocumentNumberService numbers;
  private final AlertService alerts;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param feeds feeds
   * @param runs runs
   * @param records records
   * @param handlers feed handlers of the modules
   * @param numbers run numbers
   * @param alerts alerts
   * @param notifications notifications
   * @param audit audit trail
   * @param txManager transaction manager
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public FlowInService(
      FlowInFeedRepository feeds,
      FlowInRunRepository runs,
      FlowInRecordRepository records,
      List<FlowInHandler> handlers,
      DocumentNumberService numbers,
      AlertService alerts,
      NotificationService notifications,
      AuditTrailService audit,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.feeds = feeds;
    this.runs = runs;
    this.records = records;
    this.handlers =
        handlers.stream().collect(Collectors.toMap(FlowInHandler::feedCode, Function.identity()));
    this.numbers = numbers;
    this.alerts = alerts;
    this.notifications = notifications;
    this.audit = audit;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * Runs work against a feed as one logged run. Call outside a transaction: the run, each record
   * and the outcome are committed on their own.
   *
   * @param feedCode feed
   * @param trigger what started the run
   * @param file uploaded file name and checksum, {@link FileRef#NONE} without a file
   * @param work the work; accepts its records through the context
   * @return the finished run
   */
  public FlowInRun run(
      String feedCode, Trigger trigger, FileRef file, Consumer<FlowInContext> work) {
    FlowInFeed feed = requireActive(feedCode);
    FlowInRun started =
        required(
            tx.execute(
                s ->
                    runs.save(
                        new FlowInRun(
                            feed.getCode(),
                            numbers.next("FIR-" + LocalDate.now(clock).getYear()),
                            trigger,
                            file,
                            clock.instant()))));
    Context context = new Context(started.getId(), feed.getCode(), started.getRunNo());
    String error = null;
    try {
      work.accept(context);
    } catch (BusinessRuleException | ResourceNotFoundException | IllegalStateException ex) {
      error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
    return finish(started.getId(), feed, error, context);
  }

  /**
   * Hands an uploaded file to the feed's handler as an UPLOAD run (manual upload transport).
   *
   * @param feedCode feed
   * @param file file
   * @return the finished run
   */
  public FlowInRun upload(String feedCode, FlowInFile file) {
    FlowInHandler handler = handlers.get(feedCode);
    if (handler == null) {
      throw new BusinessRuleException(
          "FLOW_IN_NO_HANDLER", "No module processes uploads of feed " + feedCode + " yet");
    }
    return run(
        feedCode,
        Trigger.UPLOAD,
        new FileRef(file.fileName(), Sha256.hex(file.content())),
        ctx -> handler.handle(file, ctx));
  }

  /**
   * Records a record that failed outside a run's work (e.g. the booking feed listener), as a
   * one-record EVENT run.
   *
   * @param feedCode feed
   * @param key idempotency key
   * @param message failure
   * @return the run
   */
  public FlowInRun failure(String feedCode, String key, String message) {
    return run(
        feedCode,
        Trigger.EVENT,
        FileRef.NONE,
        ctx ->
            ctx.accept(
                key,
                key,
                () -> {
                  throw new BusinessRuleException("FLOW_IN_RECORD_FAILED", message);
                }));
  }

  private FlowInRun finish(Long runId, FlowInFeed feed, String error, Context context) {
    FlowInRun run =
        required(
            tx.execute(
                s -> {
                  FlowInRun r = runs.findById(runId).orElseThrow();
                  context.outcomes.forEach(r::count);
                  r.finish(summary(r), error, clock.instant());
                  audit.record(
                      ENTITY,
                      r.getRunNo(),
                      AuditAction.RUN,
                      feed.getCode() + ": " + r.getMessage());
                  return r;
                }));
    if (run.getStatus() != RunStatus.SUCCEEDED) {
      tx.executeWithoutResult(s -> signalFailure(feed, run));
    }
    return run;
  }

  private void signalFailure(FlowInFeed feed, FlowInRun run) {
    String message =
        feed.getName() + " run " + run.getRunNo() + " " + run.getStatus() + ": " + run.getMessage();
    alerts.raise(
        FAILED_ALERT,
        new AlertFacts(
            null, null, ENTITY, run.getRunNo(), message, null, "FLOWIN:" + run.getRunNo()));
    notifications.notifyPermission(
        "FLOWIN_MANAGE",
        new Notice(
            "Interface run " + run.getStatus().name().toLowerCase(Locale.ROOT),
            message,
            "/operations/interfaces",
            ENTITY,
            run.getRunNo()),
        FAILED_ALERT);
  }

  private static FlowInRun required(FlowInRun run) {
    if (run == null) {
      throw new IllegalStateException("The flow-in run was not saved");
    }
    return run;
  }

  private static String summary(FlowInRun r) {
    return r.getReadCount()
        + " read, "
        + r.getOkCount()
        + " accepted, "
        + r.getDuplicateCount()
        + " duplicate(s), "
        + r.getFailedCount()
        + " failed";
  }

  private FlowInFeed requireActive(String code) {
    FlowInFeed feed = feed(code);
    if (!feed.isActive()) {
      throw new BusinessRuleException("FLOW_IN_FEED_INACTIVE", "Feed " + code + " is inactive");
    }
    return feed;
  }

  /**
   * A feed.
   *
   * @param code code
   * @return feed
   */
  public FlowInFeed feed(String code) {
    return feeds.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Feed", code));
  }

  /**
   * Every feed.
   *
   * @return feeds by partner system
   */
  public List<FlowInFeed> feeds() {
    return feeds.findAllByOrderByPartnerSystemAscCodeAsc();
  }

  /**
   * Whether a module processes uploads of a feed.
   *
   * @param code feed
   * @return true when a handler is installed
   */
  public boolean hasHandler(String code) {
    return handlers.containsKey(code);
  }

  /**
   * Changes a feed's schedule and activation.
   *
   * @param code feed
   * @param cron Spring cron, "-" for manual only
   * @param active whether runs are accepted
   * @return the feed
   */
  public FlowInFeed configure(String code, String cron, boolean active) {
    return tx.execute(
        s -> {
          FlowInFeed feed = feed(code);
          feed.configure(cron, active);
          audit.record(
              "FlowInFeed",
              code,
              AuditAction.UPDATE,
              "Schedule " + feed.getCron() + ", active " + active);
          return feed;
        });
  }

  /**
   * Runs, newest first.
   *
   * @param feedCode feed, null for all
   * @param pageable page
   * @return runs
   */
  public Page<FlowInRun> runs(String feedCode, Pageable pageable) {
    return feedCode == null || feedCode.isBlank()
        ? runs.findAllByOrderByIdDesc(pageable)
        : runs.findByFeedCodeOrderByIdDesc(feedCode, pageable);
  }

  /**
   * Records of a run.
   *
   * @param runId run
   * @param pageable page
   * @return records
   */
  public Page<FlowInRecord> records(Long runId, Pageable pageable) {
    return records.findByRunIdOrderByIdAsc(runId, pageable);
  }

  /** Run context: one transaction per record, counts on the run. */
  private final class Context implements FlowInContext {

    private final Long runId;
    private final String feedCode;
    private final String runNo;
    private final List<RecordStatus> outcomes = new ArrayList<>();

    Context(Long runId, String feedCode, String runNo) {
      this.runId = runId;
      this.feedCode = feedCode;
      this.runNo = runNo;
    }

    @Override
    public String runNo() {
      return runNo;
    }

    @Override
    public boolean accept(String idempotencyKey, String payload, Supplier<String> work) {
      String key =
          idempotencyKey.length() <= MAX_KEY
              ? idempotencyKey
              : idempotencyKey.substring(0, MAX_KEY);
      String hash = Sha256.hex(payload == null ? "" : payload);
      Optional<FlowInRecord> earlier = records.findByFeedCodeAndIdempotencyKey(feedCode, key);
      if (earlier.isPresent() && earlier.get().getStatus() == RecordStatus.ACCEPTED) {
        count(null);
        return false;
      }
      try {
        tx.executeWithoutResult(s -> save(key, hash, RecordStatus.ACCEPTED, work.get(), null));
        count(RecordStatus.ACCEPTED);
        return true;
      } catch (BusinessRuleException | ResourceNotFoundException ex) {
        tx.executeWithoutResult(s -> save(key, hash, RecordStatus.FAILED, null, ex.getMessage()));
        count(RecordStatus.FAILED);
        return false;
      }
    }

    private void save(String key, String hash, RecordStatus status, String ref, String message) {
      FlowInRecord record =
          records
              .findByFeedCodeAndIdempotencyKey(feedCode, key)
              .orElseGet(() -> new FlowInRecord(feedCode, runId, key, hash, clock.instant()));
      record.attempted(runId, hash, status, ref, message, clock.instant());
      records.save(record);
    }

    private void count(RecordStatus status) {
      outcomes.add(status);
    }
  }
}
