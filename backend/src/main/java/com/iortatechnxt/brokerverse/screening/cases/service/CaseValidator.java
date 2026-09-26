package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocumentRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.config.domain.ValidationKind;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.ValidationRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates a dispositioned case before it is routed (SNSRP-701, 802; FR-SS-060, 081) against the
 * validation rules the case started with, for the stage it leaves and its type: template complete,
 * required document types present, disposition allowed in the stage, recommendation present, STR
 * flag consistent with the disposition. A blocking rule stops the submission; a non-blocking one
 * gives a warning. The document rules are also run in advisory mode by the SLA monitor.
 */
@Component
@Transactional(readOnly = true)
public class CaseValidator {

  /** Dispositions that need the STR flag when a rule gives no parameters. */
  static final Set<String> STR_DISPOSITIONS = Set.of("TRUE_MATCH_REVIEW", "FOR_STR");

  private final ActiveConfig config;
  private final CaseReviewService reviews;
  private final CaseDocumentRepository documents;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the validator.
   *
   * @param config active configuration
   * @param reviews case reviews
   * @param documents case documents
   * @param lovs lists of values
   * @param clock clock
   */
  public CaseValidator(
      ActiveConfig config,
      CaseReviewService reviews,
      CaseDocumentRepository documents,
      LovService lovs,
      Clock clock) {
    this.config = config;
    this.reviews = reviews;
    this.documents = documents;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Checks a case leaving a stage with a disposition.
   *
   * @param c the case
   * @param stage the stage the case leaves (rules of that stage apply)
   * @param decision disposition, recommendation and STR flag
   * @return the failures, the field errors and the warnings
   */
  public Validation validate(ScreeningCase c, CaseStage stage, Decision decision) {
    List<String> blocking = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Map<String, String> fields = new LinkedHashMap<>();
    for (ValidationRules.Rule rule : rules(c, stage)) {
      List<String> messages = check(c, stage, rule, decision, fields);
      (rule.blocking() ? blocking : warnings).addAll(messages);
    }
    return new Validation(blocking, fields, warnings);
  }

  private List<ValidationRules.Rule> rules(ScreeningCase c, CaseStage stage) {
    return c.getValidationVersionId() == null
        ? List.of()
        : config
            .validationRules(c.getValidationVersionId())
            .rulesFor(stage.name(), c.getCaseType());
  }

  private List<String> check(
      ScreeningCase c,
      CaseStage stage,
      ValidationRules.Rule rule,
      Decision decision,
      Map<String, String> fields) {
    return switch (rule.rule()) {
      case TEMPLATE_COMPLETE -> templateComplete(c, rule, fields);
      case DOCUMENT_TYPES_PRESENT -> missingDocuments(c, rule.parameters());
      case DISPOSITION_ALLOWED ->
          allowed(stage, decision.disposition())
              ? List.of()
              : List.of(
                  "Disposition " + decision.disposition() + " is not allowed in stage " + stage);
      case RECOMMENDATION_PRESENT ->
          blank(decision.recommendation()) ? List.of("Write the recommendation") : List.of();
      case STR_FLAG_CONSISTENT -> strFlag(rule.parameters(), decision);
    };
  }

  private List<String> templateComplete(
      ScreeningCase c, ValidationRules.Rule rule, Map<String, String> fields) {
    Map<String, String> missing = reviews.missing(c);
    if (rule.blocking()) {
      fields.putAll(missing);
    }
    return List.copyOf(missing.values());
  }

  private List<String> strFlag(String parameters, Decision decision) {
    Set<String> needing =
        parameters == null || parameters.isBlank() ? STR_DISPOSITIONS : codes(parameters);
    return needing.contains(decision.disposition()) && !decision.strRequired()
        ? List.of(
            "Disposition "
                + label(CaseCodes.DISPOSITION_LOV, decision.disposition())
                + " requires the STR flag")
        : List.of();
  }

  /**
   * Whether a disposition belongs to a stage (SNSRP-107).
   *
   * @param stage the stage
   * @param disposition the disposition code
   * @return true when it is a value of the stage
   */
  public boolean allowed(CaseStage stage, String disposition) {
    return disposition != null
        && lovs.activeValues(CaseCodes.DISPOSITION_LOV, LocalDate.now(clock)).stream()
            .anyMatch(
                v -> v.getCode().equals(disposition) && stage.name().equals(v.getParentCode()));
  }

  /**
   * The required documents of the case's current stage that are missing (advisory, FR-SS-081).
   *
   * @param c the case
   * @return messages naming each missing document type
   */
  public List<String> missingDocuments(ScreeningCase c) {
    return rules(c, c.getStage()).stream()
        .filter(r -> r.rule() == ValidationKind.DOCUMENT_TYPES_PRESENT)
        .flatMap(r -> missingDocuments(c, r.parameters()).stream())
        .distinct()
        .toList();
  }

  private List<String> missingDocuments(ScreeningCase c, String parameters) {
    if (parameters == null || parameters.isBlank()) {
      return List.of();
    }
    Set<String> present =
        documents.findByCaseIdOrderByIdAsc(c.getId()).stream()
            .map(CaseDocument::getDocumentType)
            .collect(Collectors.toSet());
    return codes(parameters).stream()
        .filter(code -> !present.contains(code))
        .sorted()
        .map(
            code -> "Upload the " + label(CaseCodes.DOCUMENT_TYPE_LOV, code) + " before submitting")
        .toList();
  }

  private String label(String list, String code) {
    return lovs.activeValues(list, LocalDate.now(clock)).stream()
        .filter(v -> v.getCode().equals(code))
        .map(LovValue::getLabel)
        .findFirst()
        .orElse(code);
  }

  private static Set<String> codes(String parameters) {
    return Arrays.stream(parameters.split(","))
        .map(String::strip)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }

  /**
   * What a case is validated with.
   *
   * @param disposition the disposition
   * @param recommendation the recommendation or remarks
   * @param strRequired the STR flag
   */
  public record Decision(String disposition, String recommendation, boolean strRequired) {}

  /**
   * The outcome of a validation.
   *
   * @param blocking messages of failed blocking rules
   * @param fields messages by review field (template completeness)
   * @param warnings messages of failed non-blocking rules
   */
  public record Validation(
      List<String> blocking, Map<String, String> fields, List<String> warnings) {

    /** Defensive copies. */
    public Validation {
      blocking = List.copyOf(blocking);
      fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
      warnings = List.copyOf(warnings);
    }

    /**
     * Whether no blocking rule failed.
     *
     * @return true when valid
     */
    public boolean passed() {
      return blocking.isEmpty();
    }

    /**
     * The messages of the timeline entry.
     *
     * @return failures then warnings
     */
    public String summary() {
      List<String> all = new ArrayList<>(blocking);
      warnings.stream().map(w -> "Warning: " + w).forEach(all::add);
      return all.stream().filter(Objects::nonNull).collect(Collectors.joining("; "));
    }
  }
}
