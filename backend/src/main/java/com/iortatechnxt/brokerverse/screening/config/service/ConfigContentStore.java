package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.ApprovalRouteRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.AssignmentRuleRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchRuleRow;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchRuleRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskCategoryRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskRuleRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.SlaRuleRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.StrLayoutColumnRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.StrLayoutRow;
import com.iortatechnxt.brokerverse.screening.config.domain.StrLayoutRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateFieldRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateRow;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateRowRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.ValidationRuleRowRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and replaces the rows of a configuration version (design 4.1). A draft's rows are replaced
 * as a whole on each save: the old rows are deleted first, so the unique keys of the rows never
 * clash with the new ones.
 */
@Component
@Transactional
public class ConfigContentStore {

  private final MatchRuleRowRepository matchRules;
  private final RiskCategoryRowRepository categories;
  private final RiskRuleRowRepository riskRules;
  private final ApprovalRouteRowRepository routes;
  private final AssignmentRuleRowRepository assignments;
  private final SlaRuleRowRepository slas;
  private final ValidationRuleRowRepository validations;
  private final TemplateRowRepository templates;
  private final TemplateFieldRowRepository fields;
  private final StrLayoutRowRepository layouts;
  private final StrLayoutColumnRowRepository columns;

  /**
   * Creates the store.
   *
   * @param matchRules matching rules
   * @param categories risk categories
   * @param riskRules risk rules
   * @param routes approval routes
   * @param assignments assignment scenarios
   * @param slas SLA rows
   * @param validations validation rules
   * @param templates templates
   * @param fields template fields
   * @param layouts STR layouts
   * @param columns STR layout columns
   */
  public ConfigContentStore(
      MatchRuleRowRepository matchRules,
      RiskCategoryRowRepository categories,
      RiskRuleRowRepository riskRules,
      ApprovalRouteRowRepository routes,
      AssignmentRuleRowRepository assignments,
      SlaRuleRowRepository slas,
      ValidationRuleRowRepository validations,
      TemplateRowRepository templates,
      TemplateFieldRowRepository fields,
      StrLayoutRowRepository layouts,
      StrLayoutColumnRowRepository columns) {
    this.matchRules = matchRules;
    this.categories = categories;
    this.riskRules = riskRules;
    this.routes = routes;
    this.assignments = assignments;
    this.slas = slas;
    this.validations = validations;
    this.templates = templates;
    this.fields = fields;
    this.layouts = layouts;
    this.columns = columns;
  }

  /**
   * The content of a version.
   *
   * @param versionId version
   * @param type its type
   * @return content (the lists of the other types are empty)
   */
  @Transactional(readOnly = true)
  public ConfigContent load(Long versionId, ConfigType type) {
    return switch (type) {
      case MATCH_CRITERIA ->
          ConfigContent.ofMatch(
              map(
                  matchRules.findByVersionIdOrderBySortOrderAscIdAsc(versionId),
                  ConfigRowMapper::toView));
      case RISK_RULES ->
          ConfigContent.ofRisk(
              map(
                  categories.findByVersionIdOrderByTierAscIdAsc(versionId),
                  ConfigRowMapper::toView),
              map(
                  riskRules.findByVersionIdOrderByPriorityAscIdAsc(versionId),
                  ConfigRowMapper::toView));
      case APPROVAL_MATRIX ->
          ConfigContent.ofRoutes(
              map(
                  routes.findByVersionIdOrderBySortOrderAscIdAsc(versionId),
                  ConfigRowMapper::toView));
      case ASSIGNMENT_MATRIX ->
          ConfigContent.ofAssignments(
              map(
                  assignments.findByVersionIdOrderBySortOrderAscIdAsc(versionId),
                  ConfigRowMapper::toView));
      case SLA_MATRIX ->
          ConfigContent.ofSla(
              map(slas.findByVersionIdOrderByStageAscIdAsc(versionId), ConfigRowMapper::toView));
      case VALIDATION_RULES ->
          ConfigContent.ofValidation(
              map(validations.findByVersionIdOrderByIdAsc(versionId), ConfigRowMapper::toView));
      case TEMPLATE -> ConfigContent.ofTemplate(template(versionId));
      case STR_LAYOUT -> ConfigContent.ofLayout(layout(versionId));
    };
  }

  private static <R, V> List<V> map(List<R> rows, Function<R, V> mapper) {
    return rows.stream().map(mapper).toList();
  }

  private ConfigContent.Template template(Long versionId) {
    List<TemplateRow> rows = templates.findByVersionIdOrderByIdAsc(versionId);
    if (rows.isEmpty()) {
      return null;
    }
    TemplateRow t = rows.get(0);
    return new ConfigContent.Template(
        t.getTemplateType(),
        t.getName(),
        fields.findByTemplateIdOrderBySortOrderAscIdAsc(t.getId()).stream()
            .map(ConfigRowMapper::toView)
            .toList());
  }

  private ConfigContent.Layout layout(Long versionId) {
    List<StrLayoutRow> rows = layouts.findByVersionIdOrderByIdAsc(versionId);
    if (rows.isEmpty()) {
      return null;
    }
    StrLayoutRow l = rows.get(0);
    return new ConfigContent.Layout(
        l.getFormat(),
        l.getDelimiter(),
        l.getEncoding(),
        columns.findByLayoutIdOrderBySortOrderAscIdAsc(l.getId()).stream()
            .map(ConfigRowMapper::toView)
            .toList());
  }

  /**
   * Replaces the rows of a version with the content of its type.
   *
   * @param versionId version (a draft)
   * @param type its type
   * @param content new content
   */
  public void replace(Long versionId, ConfigType type, ConfigContent content) {
    switch (type) {
      case MATCH_CRITERIA -> replaceMatch(versionId, content.matchRules());
      case RISK_RULES -> {
        categories.deleteByVersionId(versionId);
        riskRules.deleteByVersionId(versionId);
        categories.saveAll(
            content.riskCategories().stream()
                .map(c -> ConfigRowMapper.toRow(versionId, c))
                .toList());
        riskRules.saveAll(
            content.riskRules().stream().map(r -> ConfigRowMapper.toRow(versionId, r)).toList());
      }
      case APPROVAL_MATRIX -> {
        routes.deleteByVersionId(versionId);
        routes.saveAll(
            content.routes().stream().map(r -> ConfigRowMapper.toRow(versionId, r)).toList());
      }
      case ASSIGNMENT_MATRIX -> {
        assignments.deleteByVersionId(versionId);
        assignments.saveAll(
            content.assignmentRules().stream()
                .map(r -> ConfigRowMapper.toRow(versionId, r))
                .toList());
      }
      case SLA_MATRIX -> {
        slas.deleteByVersionId(versionId);
        slas.saveAll(
            content.slaRules().stream().map(r -> ConfigRowMapper.toRow(versionId, r)).toList());
      }
      case VALIDATION_RULES -> {
        validations.deleteByVersionId(versionId);
        validations.saveAll(
            content.validationRules().stream()
                .map(r -> ConfigRowMapper.toRow(versionId, r))
                .toList());
      }
      case TEMPLATE -> replaceTemplate(versionId, content.template());
      default -> replaceLayout(versionId, content.layout());
    }
  }

  private void replaceMatch(Long versionId, List<MatchCriteria.Rule> rules) {
    matchRules.deleteByVersionId(versionId);
    List<MatchRuleRow> rows = new ArrayList<>();
    for (int i = 0; i < rules.size(); i++) {
      rows.add(ConfigRowMapper.toRow(versionId, (i + 1) * 10, rules.get(i)));
    }
    matchRules.saveAll(rows);
  }

  private void replaceTemplate(Long versionId, ConfigContent.Template template) {
    for (TemplateRow old : templates.findByVersionIdOrderByIdAsc(versionId)) {
      fields.deleteByTemplateId(old.getId());
    }
    templates.deleteByVersionId(versionId);
    if (template == null) {
      return;
    }
    TemplateRow row =
        templates.save(new TemplateRow(versionId, template.templateType(), template.name()));
    fields.saveAll(
        template.fields().stream().map(f -> ConfigRowMapper.toRow(row.getId(), f)).toList());
  }

  private void replaceLayout(Long versionId, ConfigContent.Layout layout) {
    for (StrLayoutRow old : layouts.findByVersionIdOrderByIdAsc(versionId)) {
      columns.deleteByLayoutId(old.getId());
    }
    layouts.deleteByVersionId(versionId);
    if (layout == null) {
      return;
    }
    StrLayoutRow row =
        layouts.save(
            new StrLayoutRow(versionId, layout.format(), layout.delimiter(), layout.encoding()));
    columns.saveAll(
        layout.columns().stream().map(c -> ConfigRowMapper.toRow(row.getId(), c)).toList());
  }
}
