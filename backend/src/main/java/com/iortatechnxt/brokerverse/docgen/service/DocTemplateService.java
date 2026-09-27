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
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Template lookup (version effective on a date), merge and new versions (BRNB.004); a version is
 * downloaded as Word and an edited Word file is read back as the draft of a new version (client
 * requirement 16).
 */
@Service
@Transactional
public class DocTemplateService {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");
  private static final String ENTITY = "DocTemplate";
  private static final String TEMPLATE = "Document template";

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
        .orElseThrow(() -> new ResourceNotFoundException(TEMPLATE, code));
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
   * A template version as a Word file: title, then the text with its placeholders.
   *
   * @param code template
   * @param versionNo version
   * @return DOCX bytes
   */
  @Transactional(readOnly = true)
  public byte[] word(String code, int versionNo) {
    return TemplateWordFile.write(version(code, versionNo));
  }

  /**
   * Reads an edited Word file as the draft of a new version, compared with the latest version:
   * placeholders the draft drops or adds are listed so the administrator can check them before
   * saving. Nothing is saved.
   *
   * @param code template (existing)
   * @param content DOCX bytes
   * @return draft
   */
  @Transactional(readOnly = true)
  public TemplateDraft readWord(String code, byte[] content) {
    List<DocTemplate> versions = templates.findByCodeOrderByVersionNoDesc(code);
    if (versions.isEmpty()) {
      throw new ResourceNotFoundException(TEMPLATE, code);
    }
    TemplateWordFile.Draft draft = TemplateWordFile.read(content);
    Set<String> before = placeholders(versions.get(0).getBody());
    Set<String> after = placeholders(draft.body());
    Set<String> missing = new TreeSet<>(before);
    missing.removeAll(after);
    Set<String> added = new TreeSet<>(after);
    added.removeAll(before);
    return new TemplateDraft(draft.title(), draft.body(), List.copyOf(missing), List.copyOf(added));
  }

  private DocTemplate version(String code, int versionNo) {
    return templates.findByCodeOrderByVersionNoDesc(code).stream()
        .filter(t -> t.getVersionNo() == versionNo)
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(TEMPLATE, code + " v" + versionNo));
  }

  private static Set<String> placeholders(String text) {
    Set<String> names = new TreeSet<>();
    Matcher m = PLACEHOLDER.matcher(text);
    while (m.find()) {
      names.add(m.group(1));
    }
    return names;
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
      throw new ResourceNotFoundException(TEMPLATE, code);
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

  /**
   * The draft of a new version read from Word.
   *
   * @param title title (first paragraph)
   * @param body text (the other paragraphs, separated by blank lines)
   * @param missingPlaceholders placeholders of the latest version the draft no longer has
   * @param addedPlaceholders placeholders the latest version does not have
   */
  public record TemplateDraft(
      String title,
      String body,
      List<String> missingPlaceholders,
      List<String> addedPlaceholders) {}
}
