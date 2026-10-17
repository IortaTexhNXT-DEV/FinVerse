package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRepository;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRun;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRun.Outcome;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRun.Result;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRunRepository;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.security.service.DataScopeLookup;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The REPORT_SUBSCRIPTIONS job (G04): runs the report subscriptions due on the business date, each
 * in its own transaction and as its subscriber. At run time the subscriber must still be an enabled
 * user holding the report's export permission, and every company and branch of the parameters must
 * be in the subscriber's data scope (a report without a company needs a scope over every company);
 * otherwise the run is recorded as DENIED. The file is archived in the report archive and e-mailed
 * to the recipients; one run is recorded per subscription and date, so running the job again for a
 * date does nothing more.
 */
@Component
public class ReportSubscriptionRunner implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "REPORT_SUBSCRIPTIONS";

  /** Outbox purpose of the e-mails. */
  public static final String PURPOSE = "REPORT_SUBSCRIPTION";

  private final ReportSubscriptionRepository subscriptions;
  private final ReportSubscriptionRunRepository runs;
  private final ReportSubscriptionService service;
  private final ReportRegistry registry;
  private final ReportService reports;
  private final ReportArchiveService archive;
  private final UserDetailsService users;
  private final DataScopeLookup scopes;
  private final MessageService messages;
  private final SubscriptionCalendar calendar;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param subscriptions subscriptions
   * @param runs runs
   * @param service parameters of a subscription
   * @param registry reports
   * @param reports report generation
   * @param archive report archive (file content)
   * @param users subscribers and their permissions
   * @param scopes data scope of the subscribers
   * @param messages outbox
   * @param calendar working days, time of day and date periods
   * @param txManager transaction manager
   * @param cron schedule ({@code brokerverse.jobs.report-subscriptions-cron})
   */
  public ReportSubscriptionRunner(
      ReportSubscriptionRepository subscriptions,
      ReportSubscriptionRunRepository runs,
      ReportSubscriptionService service,
      ReportRegistry registry,
      ReportService reports,
      ReportArchiveService archive,
      UserDetailsService users,
      DataScopeLookup scopes,
      MessageService messages,
      SubscriptionCalendar calendar,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.report-subscriptions-cron:0 5 * * * *}") String cron) {
    this.subscriptions = subscriptions;
    this.runs = runs;
    this.service = service;
    this.registry = registry;
    this.reports = reports;
    this.archive = archive;
    this.users = users;
    this.scopes = scopes;
    this.messages = messages;
    this.calendar = calendar;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Runs the report subscriptions due today and e-mails the files";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Map<Outcome, Integer> counts = new EnumMap<>(Outcome.class);
    for (ReportSubscription s : subscriptions.findByActiveTrueOrderByIdAsc()) {
      if (!s.isActive()
          || runs.existsBySubscriptionIdAndRunDate(s.getId(), businessDate)
          || !dueNow(s, businessDate)) {
        continue;
      }
      Outcome outcome = runOne(s.getId(), businessDate);
      counts.merge(outcome, 1, Integer::sum);
    }
    int total = counts.values().stream().mapToInt(Integer::intValue).sum();
    return new JobOutcome(
        total,
        total
            + " subscriptions run: "
            + counts.getOrDefault(Outcome.SUCCEEDED, 0)
            + " sent, "
            + counts.getOrDefault(Outcome.DENIED, 0)
            + " denied, "
            + counts.getOrDefault(Outcome.FAILED, 0)
            + " failed");
  }

  /**
   * Whether a subscription runs now; a subscription whose report or parameters cannot be read runs,
   * so that its failure is recorded.
   *
   * @param s subscription
   * @param date business date
   * @return true when due now
   */
  private boolean dueNow(ReportSubscription s, LocalDate date) {
    try {
      return calendar.runsNow(
          s, registry.get(s.getReportCode()).metadata(), service.parameters(s), date);
    } catch (RuntimeException e) {
      return s.isDueOn(date);
    }
  }

  /**
   * Runs one subscription on a date and records the run.
   *
   * @param subscriptionId subscription
   * @param date business date
   * @return outcome
   */
  public Outcome runOne(Long subscriptionId, LocalDate date) {
    Result result;
    try {
      Result ran = tx.execute(s -> run(subscriptions.getReferenceById(subscriptionId), date));
      result = ran == null ? new Result(Outcome.FAILED, null, 0, null, "No result") : ran;
    } catch (RuntimeException e) {
      result = new Result(Outcome.FAILED, null, 0, null, e.getMessage());
    }
    Result recorded = result;
    tx.executeWithoutResult(
        s -> {
          runs.save(new ReportSubscriptionRun(subscriptionId, date, recorded));
          subscriptions.getReferenceById(subscriptionId).ran(date);
        });
    return recorded.status();
  }

  private Result run(ReportSubscription subscription, LocalDate date) {
    UserDetails subscriber = enabledUser(subscription.getUsername());
    if (subscriber == null) {
      return denied("The subscriber is no longer an enabled user");
    }
    ReportDefinition def = registry.get(subscription.getReportCode());
    Map<String, String> parameters =
        SubscriptionCalendar.withPeriod(
            subscription.getDatePeriod(), def.metadata(), service.parameters(subscription), date);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(subscriber, null, subscriber.getAuthorities()));
    try {
      String refusal =
          ReportAccess.mayExport(def.metadata())
              ? outsideScope(def.metadata(), parameters, scopes.scopeOf(subscription.getUsername()))
              : "The subscriber no longer holds the export permission of the report";
      if (refusal != null) {
        return denied(refusal);
      }
      ExportFormat format = ExportFormat.valueOf(subscription.getFormat());
      ReportRun run = reports.generate(subscription.getReportCode(), parameters, format, null);
      Long messageId = send(subscription, def, run, date);
      return new Result(
          Outcome.SUCCEEDED, run.getId(), run.getRowCount(), messageId, run.getFileName());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private UserDetails enabledUser(String username) {
    try {
      UserDetails user = users.loadUserByUsername(username);
      return user.isEnabled() && user.isAccountNonLocked() ? user : null;
    } catch (UsernameNotFoundException e) {
      return null;
    }
  }

  /**
   * Why parameters fall outside a data scope.
   *
   * @param metadata report
   * @param parameters values by name
   * @param scope subscriber's scope
   * @return reason, null when within the scope
   */
  static String outsideScope(
      ReportMetadata metadata, Map<String, String> parameters, UserDataScope scope) {
    Long company = valueOf(metadata, parameters, ParameterType.COMPANY);
    Long branch = valueOf(metadata, parameters, ParameterType.BRANCH);
    boolean companyParameter =
        metadata.parameters().stream().anyMatch(p -> p.type() == ParameterType.COMPANY);
    if (company != null && !scope.allowsCompany(company)) {
      return "Company " + company + " is outside the subscriber's data scope";
    }
    if (scope.allCompanies()) {
      return null;
    }
    return company == null
        ? withoutCompany(branch, companyParameter)
        : outsideBranch(scope, company, branch);
  }

  private static String withoutCompany(Long branch, boolean companyParameter) {
    if (branch != null) {
      return "Branch " + branch + " needs its company in the parameters";
    }
    return companyParameter
        ? "Every company is outside the subscriber's data scope; name a company"
        : null;
  }

  private static String outsideBranch(UserDataScope scope, Long company, Long branch) {
    return branch != null && !scope.allowsBranch(company, branch)
        ? "Branch " + branch + " is outside the subscriber's data scope"
        : null;
  }

  private static Long valueOf(
      ReportMetadata metadata, Map<String, String> parameters, ParameterType type) {
    return metadata.parameters().stream()
        .filter(p -> p.type() == type)
        .map(p -> parameters.get(p.name()))
        .filter(v -> v != null && !v.isBlank())
        .map(Long::valueOf)
        .findFirst()
        .orElse(null);
  }

  private Long send(
      ReportSubscription subscription, ReportDefinition def, ReportRun run, LocalDate date) {
    ArchivedFile file =
        new ArchivedFile(run.getFileName(), run.getContentType(), archive.content(run));
    List<String> to = subscription.getRecipientList();
    return messages
        .queueEmail(
            new OutboundEmail(
                null,
                PURPOSE,
                to,
                null,
                def.metadata().title() + " of " + date,
                "Please find attached the report "
                    + def.metadata().title()
                    + " of "
                    + date
                    + ", sent on the subscription of "
                    + subscription.getUsername()
                    + ".",
                List.of(new MessageFile(file.name(), file.type(), file.content())),
                null,
                new RecordLink(
                    "ReportSubscription",
                    String.valueOf(subscription.getId()),
                    def.metadata().title())))
        .messageId();
  }

  private static Result denied(String reason) {
    return new Result(Outcome.DENIED, null, 0, null, reason);
  }

  private record ArchivedFile(String name, String type, byte[] content) {}
}
