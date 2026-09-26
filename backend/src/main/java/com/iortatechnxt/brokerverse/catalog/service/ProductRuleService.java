package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.DocumentRule;
import com.iortatechnxt.brokerverse.catalog.domain.DocumentRuleRepository;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRule;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRule.RuleKey;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRuleRepository;
import com.iortatechnxt.brokerverse.catalog.domain.FieldTarget;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RuleScope;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimum-field matrix and document checklist per product (BRNB.002/003/093, BRNB.026): rules for
 * every product, per line and per product, where the narrowest scope wins, and the check of a
 * record against them. Field rules may also check the format of a given value (BRPM.004: LOV,
 * RANGE, PATTERN), see {@link #violations}. The matrix content itself is parked (Q01/Q02): the rows
 * are maintained on the Products screen under maker-checker.
 */
@Service
@Transactional
public class ProductRuleService {

  /** Scope code of rules that apply to every product. */
  public static final String ALL_PRODUCTS = "*";

  private static final String ALTERNATIVES = "\\|";

  private final FieldRuleRepository fieldRules;
  private final DocumentRuleRepository documentRules;
  private final ProductCatalogService catalog;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final FieldValueRules valueRules;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param fieldRules field rules
   * @param documentRules document rules
   * @param catalog products and lines
   * @param lovs lists of values (document types)
   * @param audit audit trail
   * @param valueRules typed checks of given values
   * @param clock clock
   */
  public ProductRuleService(
      FieldRuleRepository fieldRules,
      DocumentRuleRepository documentRules,
      ProductCatalogService catalog,
      LovService lovs,
      AuditTrailService audit,
      FieldValueRules valueRules,
      Clock clock) {
    this.fieldRules = fieldRules;
    this.documentRules = documentRules;
    this.catalog = catalog;
    this.lovs = lovs;
    this.audit = audit;
    this.valueRules = valueRules;
    this.clock = clock;
  }

  /**
   * Every field rule.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<FieldRule> fieldRules() {
    return fieldRules.findAllByOrderByScopeAscScopeCodeAscTargetAscSortOrderAsc();
  }

  /**
   * Every document rule.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<DocumentRule> documentRules() {
    return documentRules.findAllByOrderByScopeAscScopeCodeAscDocumentTypeAsc();
  }

  /**
   * Adds a field rule, pending authorization.
   *
   * @param key scope, target and field key
   * @param label label
   * @param required mandatory flag
   * @param sortOrder order
   * @return rule
   */
  public FieldRule createFieldRule(RuleKey key, String label, boolean required, int sortOrder) {
    return createFieldRule(key, label, required, sortOrder, FieldRule.Check.PRESENCE);
  }

  /**
   * Adds a typed field rule (BRPM.004), pending authorization.
   *
   * @param key scope, target and field key
   * @param label label
   * @param required mandatory flag
   * @param sortOrder order
   * @param check what is checked on a given value
   * @return rule
   */
  public FieldRule createFieldRule(
      RuleKey key, String label, boolean required, int sortOrder, FieldRule.Check check) {
    requireScope(key.scope(), key.scopeCode());
    if (fieldRules
        .findByScopeAndScopeCodeAndTargetAndFieldKey(
            key.scope(), key.scopeCode(), key.target(), key.fieldKey())
        .isPresent()) {
      throw new DuplicateResourceException(CatalogKind.FIELD_RULE.label(), key.fieldKey());
    }
    FieldRule rule = new FieldRule(key, label, required, sortOrder);
    rule.check(check);
    valueRules.requireLovType(check);
    FieldRule saved = fieldRules.save(rule);
    audit.record(
        CatalogKind.FIELD_RULE.label(), saved.catalogReference(), AuditAction.CREATE, label);
    return saved;
  }

  /**
   * Changes a field rule, pending authorization.
   *
   * @param id rule
   * @param label label
   * @param required mandatory flag
   * @param sortOrder order
   * @return rule
   */
  public FieldRule updateFieldRule(Long id, String label, boolean required, int sortOrder) {
    FieldRule current =
        fieldRules
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.FIELD_RULE.label(), id));
    return updateFieldRule(
        id,
        label,
        required,
        sortOrder,
        new FieldRule.Check(
            current.getRuleType(),
            current.getLovType(),
            current.getMinValue(),
            current.getMaxValue(),
            current.getPattern()));
  }

  /**
   * Changes a typed field rule (BRPM.004), pending authorization.
   *
   * @param id rule
   * @param label label
   * @param required mandatory flag
   * @param sortOrder order
   * @param check what is checked on a given value
   * @return rule
   */
  public FieldRule updateFieldRule(
      Long id, String label, boolean required, int sortOrder, FieldRule.Check check) {
    FieldRule rule =
        fieldRules
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.FIELD_RULE.label(), id));
    valueRules.requireLovType(check);
    rule.update(label, required, sortOrder);
    rule.check(check);
    audit.record(
        CatalogKind.FIELD_RULE.label(),
        rule.catalogReference(),
        AuditAction.UPDATE,
        label + (required ? " mandatory" : " optional"));
    return rule;
  }

  /**
   * Adds a document rule, pending authorization.
   *
   * @param scope scope
   * @param scopeCode "*", line or product
   * @param documentType document type (list DOCUMENT_TYPE)
   * @param required mandatory flag
   * @return rule
   */
  public DocumentRule createDocumentRule(
      RuleScope scope, String scopeCode, String documentType, boolean required) {
    requireScope(scope, scopeCode);
    lovs.requireValid("DOCUMENT_TYPE", documentType, LocalDate.now(clock));
    if (documentRules
        .findByScopeAndScopeCodeAndDocumentType(scope, scopeCode, documentType)
        .isPresent()) {
      throw new DuplicateResourceException(CatalogKind.DOCUMENT_RULE.label(), documentType);
    }
    DocumentRule saved =
        documentRules.save(new DocumentRule(scope, scopeCode, documentType, required));
    audit.record(
        CatalogKind.DOCUMENT_RULE.label(),
        saved.catalogReference(),
        AuditAction.CREATE,
        saved.catalogDescription());
    return saved;
  }

  /**
   * Changes the mandatory flag of a document rule, pending authorization.
   *
   * @param id rule
   * @param required mandatory flag
   * @return rule
   */
  public DocumentRule updateDocumentRule(Long id, boolean required) {
    DocumentRule rule =
        documentRules
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException(CatalogKind.DOCUMENT_RULE.label(), id));
    rule.update(required);
    audit.record(
        CatalogKind.DOCUMENT_RULE.label(),
        rule.catalogReference(),
        AuditAction.UPDATE,
        rule.catalogDescription());
    return rule;
  }

  /**
   * Checks the given values of a record against the typed field rules of its product (BRPM.004): a
   * value outside its list, range or pattern is an error. Missing values are the minimum-field
   * matrix's concern ({@link #missingFields}).
   *
   * @param product product
   * @param values given values of the record and its items
   * @return field errors by field path ("items[0].yearModel"); empty when valid
   */
  @Transactional(readOnly = true)
  public Map<String, String> violations(RiskProduct product, FieldValues values) {
    return valueRules.violations(effectiveFieldRules(product), values);
  }

  private void requireScope(RuleScope scope, String scopeCode) {
    if (scope == RuleScope.LINE) {
      catalog.requireLine(scopeCode);
    } else if (scope == RuleScope.PRODUCT) {
      catalog.requireProduct(scopeCode);
    } else if (!ALL_PRODUCTS.equals(scopeCode)) {
      throw new BusinessRuleException(
          "RULE_SCOPE_INVALID", "Rules for every product use the scope code '*'");
    }
  }

  /**
   * The field rules that apply to a product: authorized rows of every product, its line and the
   * product itself; for the same target and field key the narrowest scope wins.
   *
   * @param product product
   * @return effective rules in display order (required and relaxed)
   */
  @Transactional(readOnly = true)
  public List<FieldRule> effectiveFieldRules(RiskProduct product) {
    List<FieldRule> rows =
        fieldRules.findByScopeCodeInOrderBySortOrderAscIdAsc(scopeCodes(product)).stream()
            .filter(r -> r.isActive() && applies(r.getScope(), r.getScopeCode(), product))
            .toList();
    return narrowest(rows, r -> r.getTarget() + ":" + r.getFieldKey(), FieldRule::getScope)
        .values()
        .stream()
        .sorted(Comparator.comparing(FieldRule::getTarget).thenComparing(FieldRule::getSortOrder))
        .toList();
  }

  /**
   * The document types mandatory for a product (narrowest scope wins).
   *
   * @param product product
   * @return mandatory document type codes
   */
  @Transactional(readOnly = true)
  public Set<String> requiredDocuments(RiskProduct product) {
    List<DocumentRule> rows =
        documentRules.findByScopeCodeIn(scopeCodes(product)).stream()
            .filter(r -> r.isActive() && applies(r.getScope(), r.getScopeCode(), product))
            .toList();
    return narrowest(rows, DocumentRule::getDocumentType, DocumentRule::getScope).values().stream()
        .filter(DocumentRule::isRequired)
        .map(DocumentRule::getDocumentType)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  /**
   * Checks a record against the minimum-field matrix of its product.
   *
   * @param product product
   * @param presence fields holding a value
   * @return field errors by field path ("clientId", "items[0].engineNo"); empty when complete
   */
  @Transactional(readOnly = true)
  public Map<String, String> missingFields(RiskProduct product, FieldPresence presence) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldRule rule : effectiveFieldRules(product)) {
      if (!rule.isRequired()) {
        continue;
      }
      String[] keys = rule.getFieldKey().split(ALTERNATIVES);
      if (rule.getTarget() == FieldTarget.ACCOUNT) {
        if (noneOf(keys, presence.record())) {
          errors.put(keys[0], rule.getLabel() + " is required");
        }
      } else {
        addItemErrors(rule, keys, presence.items(), errors);
      }
    }
    return errors;
  }

  private static void addItemErrors(
      FieldRule rule, String[] keys, List<Set<String>> items, Map<String, String> errors) {
    for (int i = 0; i < items.size(); i++) {
      if (noneOf(keys, items.get(i))) {
        errors.put(
            "items[" + i + "]." + keys[0],
            "Item " + (i + 1) + ": " + rule.getLabel() + " is required");
      }
    }
  }

  private static boolean noneOf(String[] keys, Set<String> present) {
    for (String key : keys) {
      if (present.contains(key)) {
        return false;
      }
    }
    return true;
  }

  private static List<String> scopeCodes(RiskProduct product) {
    return List.of(ALL_PRODUCTS, product.getLineCode(), product.getCode());
  }

  private static boolean applies(RuleScope scope, String scopeCode, RiskProduct product) {
    return switch (scope) {
      case ALL -> ALL_PRODUCTS.equals(scopeCode);
      case LINE -> product.getLineCode().equals(scopeCode);
      case PRODUCT -> product.getCode().equals(scopeCode);
    };
  }

  private static <R> Map<String, R> narrowest(
      List<R> rows, Function<R, String> key, Function<R, RuleScope> scope) {
    Map<String, R> result = new LinkedHashMap<>();
    for (R row : rows) {
      result.merge(
          key.apply(row),
          row,
          (a, b) -> scope.apply(b).ordinal() > scope.apply(a).ordinal() ? b : a);
    }
    return result;
  }
}
