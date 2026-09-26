package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseAnswer;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseAnswer.AnswerValue;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseAnswerRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseReview;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseReviewRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.config.domain.FieldDataType;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate.Field;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The guided review of a case (SNSRP-501, 104; FR-SS-050): the template of the case's type in the
 * version in force when the review started, mandatory fields marked, drafts saved at any time with
 * each wrong value flagged next to its field, and the completeness check of the submission.
 */
@Service
@Transactional
public class CaseReviewService {

  private static final Set<CaseStage> EDITABLE =
      EnumSet.of(CaseStage.INVESTIGATION, CaseStage.RETURNED);

  private final CaseReviewRepository reviews;
  private final CaseAnswerRepository answers;
  private final ActiveConfig config;
  private final LovService lovs;
  private final CaseAccess access;
  private final CaseTimeline timeline;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param reviews reviews
   * @param answers answers
   * @param config active configuration (templates)
   * @param lovs lists of values
   * @param access case access
   * @param timeline case timeline
   * @param audit audit trail
   * @param clock clock
   */
  public CaseReviewService(
      CaseReviewRepository reviews,
      CaseAnswerRepository answers,
      ActiveConfig config,
      LovService lovs,
      CaseAccess access,
      CaseTimeline timeline,
      AuditTrailService audit,
      Clock clock) {
    this.reviews = reviews;
    this.answers = answers;
    this.config = config;
    this.lovs = lovs;
    this.access = access;
    this.timeline = timeline;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Starts the review of a new case on the template of its type in force today (SNSRP-104).
   *
   * @param c the case
   * @return the review, empty when no template of the type is in force
   */
  public Optional<CaseReview> start(ScreeningCase c) {
    Optional<CaseReview> existing = reviews.findByCaseId(c.getId());
    if (existing.isPresent()) {
      return existing;
    }
    return config
        .template(c.getCompanyId(), TemplateType.valueOf(c.getTemplateType()), LocalDate.now(clock))
        .map(t -> reviews.save(new CaseReview(c.getId(), t.version().id(), c.getTemplateType())));
  }

  /**
   * The review of a case with its template and answers.
   *
   * @param c the case
   * @return the review form, empty when the case has no review
   */
  @Transactional(readOnly = true)
  public Optional<ReviewForm> form(ScreeningCase c) {
    return reviews
        .findByCaseId(c.getId())
        .map(r -> new ReviewForm(r, config.template(r.getTemplateVersionId()), raw(r)));
  }

  /**
   * Saves the answers as a draft; values of the wrong type are refused field by field.
   *
   * @param c the case
   * @param values raw values by field code (fields not given keep their answer)
   * @return the review form
   */
  public ReviewForm save(ScreeningCase c, Map<String, String> values) {
    access.requireActor(c, "Save Draft", EDITABLE);
    CaseReview review =
        start(c)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "SCR_NO_TEMPLATE",
                        "No " + c.getTemplateType() + " template is in force; ask Compliance"));
    ReviewTemplate template = config.template(review.getTemplateVersionId());
    Map<String, CaseAnswer> stored = stored(review);
    Map<String, String> errors = new LinkedHashMap<>();
    for (Field field : template.fields()) {
      if (!values.containsKey(field.code())) {
        continue;
      }
      ReviewValues.Parsed parsed =
          ReviewValues.parse(field, values.get(field.code()), listCodes(field));
      if (parsed.error() != null) {
        errors.put(field.code(), parsed.error());
      } else {
        stored
            .computeIfAbsent(
                field.code(), code -> answers.save(new CaseAnswer(review.getId(), code)))
            .set(parsed.value());
      }
    }
    if (!errors.isEmpty()) {
      throw new FieldValidationException(
          "SCR_REVIEW_INVALID", "Correct the flagged fields of the review", errors);
    }
    review.reopen();
    timeline.record(c, CaseEventType.REVIEW_SAVED, EventFacts.remarks(template.name()));
    audit.record(CaseCodes.ENTITY, c.getCaseNo(), AuditAction.UPDATE, "Review saved as draft");
    return new ReviewForm(review, template, raw(review));
  }

  /**
   * The mandatory fields that are not answered (FR-SS-050: "&lt;Field&gt; is required").
   *
   * @param c the case
   * @return messages by field code, empty when complete or when the case has no review
   */
  @Transactional(readOnly = true)
  public Map<String, String> missing(ScreeningCase c) {
    Optional<CaseReview> review = reviews.findByCaseId(c.getId());
    if (review.isEmpty()) {
      return Map.of();
    }
    ReviewTemplate template = config.template(review.get().getTemplateVersionId());
    Map<String, CaseAnswer> stored = stored(review.get());
    Map<String, String> missing = new LinkedHashMap<>();
    for (Field field : template.fields()) {
      AnswerValue value =
          Optional.ofNullable(stored.get(field.code()))
              .map(CaseAnswer::value)
              .orElse(AnswerValue.BLANK);
      if (!ReviewValues.complete(field, value)) {
        missing.put(field.code(), field.label() + " is required");
      }
    }
    return missing;
  }

  /**
   * Marks the review submitted with the case.
   *
   * @param c the case
   */
  public void submitted(ScreeningCase c) {
    reviews.findByCaseId(c.getId()).ifPresent(r -> r.submit(access.user(), clock.instant()));
  }

  /**
   * Re-opens the review of a returned or re-opened case.
   *
   * @param c the case
   */
  public void reopen(ScreeningCase c) {
    reviews.findByCaseId(c.getId()).ifPresent(CaseReview::reopen);
  }

  private Map<String, CaseAnswer> stored(CaseReview review) {
    return answers.findByReviewId(review.getId()).stream()
        .collect(
            Collectors.toMap(
                CaseAnswer::getFieldCode, Function.identity(), (a, b) -> a, HashMap::new));
  }

  private Map<String, String> raw(CaseReview review) {
    Map<String, String> raw = new LinkedHashMap<>();
    stored(review)
        .forEach((code, a) -> ReviewValues.raw(a.value()).ifPresent(v -> raw.put(code, v)));
    return raw;
  }

  private Set<String> listCodes(Field field) {
    if (field.dataType() != FieldDataType.LOV || field.lovType() == null) {
      return Set.of();
    }
    return lovs.activeValues(field.lovType(), LocalDate.now(clock)).stream()
        .map(LovValue::getCode)
        .collect(Collectors.toSet());
  }

  /**
   * A review with its template and answers.
   *
   * @param review the review
   * @param template the template version
   * @param values raw values by field code
   */
  public record ReviewForm(
      CaseReview review, ReviewTemplate template, Map<String, String> values) {}
}
