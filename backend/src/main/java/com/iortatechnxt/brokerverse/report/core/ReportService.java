package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.report.render.PrintOptions;
import com.iortatechnxt.brokerverse.report.render.ReportContext;
import com.iortatechnxt.brokerverse.report.render.ReportRenderer;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Runs and exports reports with permission checks and audit logging of every run and export. */
@Service
@Transactional(readOnly = true)
public class ReportService {

  private static final String DEFAULT_COMPANY = "iNXT BrokerVerse";
  private static final String COMPANY_PARAM = "companyId";
  private static final String ID_SEPARATOR = " – ";

  private final ReportRegistry registry;
  private final ReportArchiveService archive;
  private final Map<ExportFormat, ReportRenderer> renderers = new EnumMap<>(ExportFormat.class);
  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param registry report catalogue
   * @param archive archive of generated reports
   * @param renderers export renderers
   * @param companies companies (names in headers and echo)
   * @param branches branches (names in the echo)
   * @param audit audit trail
   * @param currentUser current user
   * @param parameters system parameters (report footer)
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ReportService(
      ReportRegistry registry,
      ReportArchiveService archive,
      List<ReportRenderer> renderers,
      CompanyRepository companies,
      BranchRepository branches,
      AuditTrailService audit,
      CurrentUser currentUser,
      SystemParameterService parameters,
      Clock clock) {
    this.registry = registry;
    this.archive = archive;
    renderers.forEach(r -> this.renderers.put(r.format(), r));
    this.companies = companies;
    this.branches = branches;
    this.audit = audit;
    this.currentUser = currentUser;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Lists the reports the current user may run.
   *
   * @return catalogue
   */
  public List<ReportMetadata> catalogue() {
    return registry.catalogue().stream().filter(ReportAccess::mayView).toList();
  }

  /**
   * Runs a report.
   *
   * @param code report code
   * @param rawParams raw parameters
   * @return result
   */
  @Transactional
  public ReportResult run(String code, Map<String, String> rawParams) {
    ReportDefinition def = authorized(code);
    ReportParameters params =
        ReportParameters.validate(def.metadata(), rawParams, clock, this::displayValue);
    ReportResult result = def.generate(params);
    audit.record("Report", code, AuditAction.RUN, "Ran report " + String.join(", ", params.echo()));
    archive.viewed(def.metadata(), params.echo(), result);
    return result;
  }

  /**
   * Runs and renders a report.
   *
   * @param code report code
   * @param rawParams raw parameters
   * @param format export format
   * @return rendered file
   */
  @Transactional
  public RenderedReport export(String code, Map<String, String> rawParams, ExportFormat format) {
    return export(code, rawParams, format, ExportOptions.NONE);
  }

  /**
   * Runs and renders a report with print options and column filters (FRBS 2.4.4, 2.4.9).
   *
   * @param code report code
   * @param rawParams raw parameters
   * @param format export format
   * @param options print options and column filters
   * @return rendered file
   */
  @Transactional
  public RenderedReport export(
      String code, Map<String, String> rawParams, ExportFormat format, ExportOptions options) {
    ReportDefinition def = authorized(code);
    if (!ReportAccess.mayExport(def.metadata())) {
      throw new AccessDeniedException("Not permitted to export report " + code);
    }
    ReportParameters params =
        ReportParameters.validate(def.metadata(), rawParams, clock, this::displayValue);
    ReportResult result = options.filter(def.generate(params));
    ReportContext ctx = context(params).withPrint(options.print());
    byte[] content = renderers.get(format).render(result, ctx);
    List<String> echo = new ArrayList<>(params.echo());
    if (format == ExportFormat.PDF && !options.print().echo().isEmpty()) {
      echo.add(options.print().echo());
    }
    audit.record(
        "Report", code, AuditAction.EXPORT, "Exported " + format + " " + String.join(", ", echo));
    String fileName = code + "." + format.extension();
    archive.exported(
        def.metadata(),
        echo,
        result,
        new RunFile(format.name(), fileName, format.contentType(), content));
    return new RenderedReport(fileName, format.contentType(), content);
  }

  /**
   * Runs a report for a batch and keeps its result for rendering (FRBS 2.4.5).
   *
   * @param code report code
   * @param rawParams shared parameters (those the report does not declare are ignored)
   * @return result
   */
  @Transactional
  public ReportResult runForExport(String code, Map<String, String> rawParams) {
    ReportDefinition def = authorized(code);
    if (!ReportAccess.mayExport(def.metadata())) {
      throw new AccessDeniedException("Not permitted to export report " + code);
    }
    Map<String, String> own = new HashMap<>();
    def.metadata()
        .parameters()
        .forEach(
            spec -> {
              String value = rawParams.get(spec.name());
              if (value != null) {
                own.put(spec.name(), value);
              }
            });
    ReportParameters params =
        ReportParameters.validate(def.metadata(), own, clock, this::displayValue);
    ReportResult result = def.generate(params);
    audit.record(
        "Report", code, AuditAction.EXPORT, "Batch export " + String.join(", ", params.echo()));
    return result;
  }

  /**
   * Renders a result with the print context of the current user.
   *
   * @param result report result
   * @param format format
   * @param print PDF print options
   * @param companyId company named in the header, may be null
   * @return file bytes
   */
  public byte[] render(
      ReportResult result, ExportFormat format, PrintOptions print, Long companyId) {
    String company =
        companyId == null
            ? DEFAULT_COMPANY
            : companies.findById(companyId).map(Company::getName).orElse(DEFAULT_COMPANY);
    ReportContext ctx =
        new ReportContext(
            company,
            currentUser.username(),
            clock.instant(),
            parameters.text(SystemParameterService.REPORT_FOOTER_TEXT, ""),
            print);
    return renderers.get(format).render(result, ctx);
  }

  /**
   * Runs, renders and archives a report as a scheduled file (BRCLXN.024-029, 045): no user
   * permission check (the caller is a {@code ManagedJob}); the file may be downloaded from {@code
   * availableFrom} on through the report archive.
   *
   * @param code report code
   * @param rawParams raw parameters
   * @param format file format
   * @param availableFrom when users may download it; null = at once
   * @return the archived run
   */
  @Transactional
  public ReportRun generate(
      String code, Map<String, String> rawParams, ExportFormat format, Instant availableFrom) {
    ReportDefinition def = registry.get(code);
    ReportParameters params =
        ReportParameters.validate(def.metadata(), rawParams, clock, this::displayValue);
    ReportResult result = def.generate(params);
    ReportContext ctx = context(params);
    return archive.archiveGenerated(
        code,
        params.echo(),
        result.rows().size(),
        new RunFile(
            format.name(),
            code + "." + format.extension(),
            format.contentType(),
            renderers.get(format).render(result, ctx)),
        availableFrom);
  }

  /**
   * Company and branch ids as "code – name" in the parameter echo of the screen and every export;
   * other values as entered. An unknown id is shown as entered.
   */
  private String displayValue(ParameterSpec spec, String value) {
    return switch (spec.type()) {
      case COMPANY ->
          companies
              .findById(Long.valueOf(value))
              .map(c -> c.getCode() + ID_SEPARATOR + c.getName())
              .orElse(value);
      case BRANCH ->
          branches
              .findById(Long.valueOf(value))
              .map(b -> b.getCode() + ID_SEPARATOR + b.getName())
              .orElse(value);
      default -> value;
    };
  }

  private ReportContext context(ReportParameters params) {
    return new ReportContext(
        companyName(params),
        currentUser.username(),
        clock.instant(),
        parameters.text(SystemParameterService.REPORT_FOOTER_TEXT, ""));
  }

  private String companyName(ReportParameters params) {
    return params
        .optionalLong(COMPANY_PARAM)
        .flatMap(companies::findById)
        .map(Company::getName)
        .orElse(DEFAULT_COMPANY);
  }

  private ReportDefinition authorized(String code) {
    ReportDefinition def = registry.get(code);
    if (!ReportAccess.mayView(def.metadata())) {
      throw new AccessDeniedException("Not permitted to run report " + code);
    }
    return def;
  }

  /**
   * Rendered report file.
   *
   * @param fileName suggested file name
   * @param contentType MIME type
   * @param content bytes
   */
  public record RenderedReport(String fileName, String contentType, byte[] content) {}
}
