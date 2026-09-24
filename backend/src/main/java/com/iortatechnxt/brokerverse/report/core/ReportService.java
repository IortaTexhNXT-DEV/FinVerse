package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.report.render.ReportContext;
import com.iortatechnxt.brokerverse.report.render.ReportRenderer;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
   * @param renderers export renderers
   * @param companies companies (names in headers and echo)
   * @param branches branches (names in the echo)
   * @param audit audit trail
   * @param currentUser current user
   * @param parameters system parameters (report footer)
   * @param clock clock
   */
  public ReportService(
      ReportRegistry registry,
      List<ReportRenderer> renderers,
      CompanyRepository companies,
      BranchRepository branches,
      AuditTrailService audit,
      CurrentUser currentUser,
      SystemParameterService parameters,
      Clock clock) {
    this.registry = registry;
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
    Set<String> granted = grantedAuthorities();
    return registry.catalogue().stream()
        .filter(m -> granted.contains(m.permission().name()))
        .toList();
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
    ReportDefinition def = authorized(code);
    ReportParameters params =
        ReportParameters.validate(def.metadata(), rawParams, clock, this::displayValue);
    ReportResult result = def.generate(params);
    ReportContext ctx =
        new ReportContext(
            companyName(params),
            currentUser.username(),
            clock.instant(),
            parameters.text(SystemParameterService.REPORT_FOOTER_TEXT, ""));
    byte[] content = renderers.get(format).render(result, ctx);
    audit.record(
        "Report",
        code,
        AuditAction.EXPORT,
        "Exported " + format + " " + String.join(", ", params.echo()));
    return new RenderedReport(code + "." + format.extension(), format.contentType(), content);
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

  private String companyName(ReportParameters params) {
    return params
        .optionalLong(COMPANY_PARAM)
        .flatMap(companies::findById)
        .map(Company::getName)
        .orElse(DEFAULT_COMPANY);
  }

  private ReportDefinition authorized(String code) {
    ReportDefinition def = registry.get(code);
    if (!grantedAuthorities().contains(def.metadata().permission().name())) {
      throw new AccessDeniedException("Not permitted to run report " + code);
    }
    return def;
  }

  private static Set<String> grantedAuthorities() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return Set.of();
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
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
