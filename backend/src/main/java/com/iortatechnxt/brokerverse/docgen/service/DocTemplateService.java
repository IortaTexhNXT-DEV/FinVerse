package com.iortatechnxt.brokerverse.docgen.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.docgen.domain.DocTemplate;
import com.iortatechnxt.brokerverse.docgen.domain.DocTemplateRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Template lookup (version effective on a date), merge and new versions (BRNB.004). */
@Service
@Transactional
public class DocTemplateService {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");
  private static final String ENTITY = "DocTemplate";

  private final DocTemplateRepository templates;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param templates templates
   * @param audit audit trail
   */
  public DocTemplateService(DocTemplateRepository templates, AuditTrailService audit) {
    this.templates = templates;
    this.audit = audit;
  }

  /**
   * The version of a template in force on a date.
   *
   * @param code template
   * @param date date
   * @return version
   */
  @Transactional(readOnly = true)
  public DocTemplate current(String code, LocalDate date) {
    return templates.findByCodeOrderByVersionNoDesc(code).stream()
        .filter(t -> t.isActive() && !t.getEffectiveFrom().isAfter(date))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Document template", code));
  }

  /**
   * Merges the template in force with values; unknown placeholders become empty.
   *
   * @param code template
   * @param date date
   * @param values placeholder values
   * @return merged text with the version used
   */
  @Transactional(readOnly = true)
  public MergedText merge(String code, LocalDate date, Map<String, ?> values) {
    DocTemplate t = current(code, date);
    return new MergedText(t.getCode(), t.getVersionNo(), t.getTitle(), fill(t.getBody(), values));
  }

  /**
   * Replaces {@code {{name}}} placeholders.
   *
   * @param text text
   * @param values values
   * @return merged text
   */
  public static String fill(String text, Map<String, ?> values) {
    Matcher m = PLACEHOLDER.matcher(text);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      Object v = values.get(m.group(1));
      m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : v.toString()));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  /**
   * Every template version.
   *
   * @return versions
   */
  @Transactional(readOnly = true)
  public List<DocTemplate> all() {
    return templates.findAllByOrderByCodeAscVersionNoDesc();
  }

  /**
   * Adds a new version of a template.
   *
   * @param code template (existing)
   * @param title title
   * @param body text
   * @param effectiveFrom first date of use
   * @return the new version
   */
  public DocTemplate newVersion(String code, String title, String body, LocalDate effectiveFrom) {
    List<DocTemplate> versions = templates.findByCodeOrderByVersionNoDesc(code);
    if (versions.isEmpty()) {
      throw new ResourceNotFoundException("Document template", code);
    }
    if (body == null || body.isBlank()) {
      throw new BusinessRuleException("TEMPLATE_BODY_REQUIRED", "Enter the template text");
    }
    DocTemplate saved =
        templates.save(
            new DocTemplate(code, versions.get(0).getVersionNo() + 1, title, body, effectiveFrom));
    audit.record(
        ENTITY,
        code + " v" + saved.getVersionNo(),
        AuditAction.CREATE,
        "New version effective " + effectiveFrom);
    return saved;
  }
}
