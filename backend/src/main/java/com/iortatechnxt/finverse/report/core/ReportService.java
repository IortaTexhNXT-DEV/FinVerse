package com.iortatechnxt.finverse.report.core;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import com.iortatechnxt.finverse.report.render.ReportContext;
import com.iortatechnxt.finverse.report.render.ReportRenderer;
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

  private static final String DEFAULT_COMPANY = "iNXT FinVerse";
  private static final String COMPANY_PARAM = "companyId";

  private final ReportRegistry registry;
  private final Map<ExportFormat, ReportRenderer> renderers = new EnumMap<>(ExportFormat.class);
  private final OrganizationService organization;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param registry report catalogue
   * @param renderers export renderers
   * @param organization organization service
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ReportService(
      ReportRegistry registry,
      List<ReportRenderer> renderers,
      OrganizationService organization,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.registry = registry;
    renderers.forEach(r -> this.renderers.put(r.format(), r));
    this.organization = organization;
    this.audit = audit;
    this.currentUser = currentUser;
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
    ReportParameters params = ReportParameters.validate(def.metadata(), rawParams, clock);
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
    ReportParameters params = ReportParameters.validate(def.metadata(), rawParams, clock);
    ReportResult result = def.generate(params);
    ReportContext ctx =
        new ReportContext(companyName(params), currentUser.username(), clock.instant());
    byte[] content = renderers.get(format).render(result, ctx);
    audit.record(
        "Report",
        code,
        AuditAction.EXPORT,
        "Exported " + format + " " + String.join(", ", params.echo()));
    return new RenderedReport(code + "." + format.extension(), format.contentType(), content);
  }

  private String companyName(ReportParameters params) {
    return params
        .optionalLong(COMPANY_PARAM)
        .map(id -> organization.getCompany(id).getName())
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
