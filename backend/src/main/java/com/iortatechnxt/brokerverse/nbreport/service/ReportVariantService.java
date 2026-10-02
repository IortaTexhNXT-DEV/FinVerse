package com.iortatechnxt.brokerverse.nbreport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbreport.domain.ReportVariant;
import com.iortatechnxt.brokerverse.nbreport.domain.ReportVariantRepository;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved report variants (BRNB.057, "dynamic reports" as saved variants while the builder is parked,
 * Q40): a user saves the parameters of a report he may run under a name, optionally shared with the
 * other users who may run it, and applies or deletes it later. Every change is audited.
 */
@Service
@Transactional
public class ReportVariantService {

  private static final String ENTITY = "ReportVariant";
  private static final TypeReference<Map<String, String>> PARAMS = new TypeReference<>() {};

  private final ReportVariantRepository variants;
  private final ReportService reports;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final ObjectMapper json;

  /**
   * Creates the service.
   *
   * @param variants variants
   * @param reports report catalogue (what the user may run)
   * @param currentUser current user
   * @param audit audit trail
   * @param json JSON mapper
   */
  public ReportVariantService(
      ReportVariantRepository variants,
      ReportService reports,
      CurrentUser currentUser,
      AuditTrailService audit,
      ObjectMapper json) {
    this.variants = variants;
    this.reports = reports;
    this.currentUser = currentUser;
    this.audit = audit;
    this.json = json;
  }

  /**
   * The variants of a report the user sees: his own and the shared ones.
   *
   * @param reportCode report
   * @return variants by name
   */
  @Transactional(readOnly = true)
  public List<ReportVariant> visible(String reportCode) {
    return variants.visibleTo(reportCode, currentUser.username());
  }

  /**
   * Saves the user's variant under a name, replacing his variant of the same name.
   *
   * @param reportCode report
   * @param name name
   * @param parameters parameter values
   * @param shared whether other users see it
   * @return variant
   */
  public ReportVariant save(
      String reportCode, String name, Map<String, String> parameters, boolean shared) {
    ReportMetadata report = runnable(reportCode);
    String owner = currentUser.username();
    ReportVariant variant =
        variants
            .findByOwnerIgnoreCaseAndReportCodeAndNameIgnoreCase(owner, reportCode, name.strip())
            .orElseGet(() -> new ReportVariant(owner, reportCode, name));
    boolean created = variant.getId() == null;
    variant.save(write(known(report, parameters)), shared);
    ReportVariant saved = variants.save(variant);
    audit.record(
        ENTITY,
        saved.getId(),
        created ? AuditAction.CREATE : AuditAction.UPDATE,
        "Saved variant '"
            + saved.getName()
            + "' of report "
            + reportCode
            + (shared ? " (shared)" : ""));
    return saved;
  }

  /**
   * Deletes one of the user's variants.
   *
   * @param id variant
   */
  public void delete(Long id) {
    ReportVariant variant =
        variants.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    if (!CurrentUser.sameUser(variant.getOwner(), currentUser.username())) {
      throw new BusinessRuleException(
          "VARIANT_NOT_OWNER", "Only the user who saved a report variant may delete it");
    }
    variants.delete(variant);
    audit.record(
        ENTITY,
        id,
        AuditAction.DEACTIVATE,
        "Deleted variant '" + variant.getName() + "' of report " + variant.getReportCode());
  }

  /**
   * The saved parameter values of a variant.
   *
   * @param variant variant
   * @return parameter name to value
   */
  public Map<String, String> parameters(ReportVariant variant) {
    try {
      return json.readValue(variant.getParameters(), PARAMS);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unreadable report variant " + variant.getId(), e);
    }
  }

  private ReportMetadata runnable(String reportCode) {
    return reports.catalogue().stream()
        .filter(m -> m.code().equals(reportCode))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "REPORT_NOT_AVAILABLE", "Report " + reportCode + " is not available to you"));
  }

  /** Keeps the report's own parameters, except the company (chosen in the header at run time). */
  private static Map<String, String> known(ReportMetadata report, Map<String, String> values) {
    Map<String, String> kept = new TreeMap<>();
    report.parameters().stream()
        .filter(p -> !"companyId".equals(p.name()) && values.get(p.name()) != null)
        .forEach(p -> kept.put(p.name(), values.get(p.name())));
    return kept;
  }

  private String write(Map<String, String> parameters) {
    try {
      return json.writeValueAsString(parameters);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Report parameters cannot be saved", e);
    }
  }
}
