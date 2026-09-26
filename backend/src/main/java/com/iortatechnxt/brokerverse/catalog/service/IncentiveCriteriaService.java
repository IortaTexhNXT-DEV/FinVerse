package com.iortatechnxt.brokerverse.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria.Details;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteriaRepository;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveScope;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.version.IncentiveCriteriaChanged;
import com.iortatechnxt.brokerverse.catalog.service.version.IncentiveCriteriaChanged.Change;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Incentive criteria on the maintained products matrix (PMADD07/08; PRODUCT_MAINTENANCE_DESIGN
 * sections 4.3 and 9.1): created, amended and deactivated by an Incentive Maintenance user
 * (INCENTIVE_CRITERIA_MAINTAIN), authorised by another user (PRODUCT_AUTHORIZE, My Approvals). Only
 * active products and insurers are accepted, incomplete set-ups and overlapping periods are
 * refused, an active criterion is changed by a successor row (history kept) and every change is
 * audited. Booking asks {@link #matching} for the criteria codes of an invoice.
 */
@Service
@Transactional
public class IncentiveCriteriaService implements CatalogRecordHook {

  /** Permission of the Incentive Maintenance user. */
  public static final String MAINTAIN = "INCENTIVE_CRITERIA_MAINTAIN";

  private static final String LABEL = CatalogKind.INCENTIVE_CRITERIA.label();

  private final IncentiveCriteriaRepository criteria;
  private final ProductCatalogService catalog;
  private final InsurerService insurers;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;
  private final ObjectMapper json;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param criteria criteria
   * @param catalog products
   * @param insurers insurers
   * @param lovs lists of values (incentive type)
   * @param audit audit trail
   * @param currentUser current user
   * @param events event publisher
   * @param json JSON (rule parameters)
   * @param clock clock
   */
  public IncentiveCriteriaService(
      IncentiveCriteriaRepository criteria,
      ProductCatalogService catalog,
      InsurerService insurers,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      ApplicationEventPublisher events,
      ObjectMapper json,
      Clock clock) {
    this.criteria = criteria;
    this.catalog = catalog;
    this.insurers = insurers;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.events = events;
    this.json = json;
    this.clock = clock;
  }

  /**
   * Every row of a company's criteria (current and history), by code and newest first.
   *
   * @param companyId company
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<IncentiveCriteria> list(Long companyId) {
    return criteria.findByCompanyIdOrderByCodeAscEffectiveFromDesc(companyId);
  }

  /**
   * One row.
   *
   * @param id id
   * @return row
   */
  @Transactional(readOnly = true)
  public IncentiveCriteria get(Long id) {
    return criteria.findById(id).orElseThrow(() -> new ResourceNotFoundException(LABEL, id));
  }

  /**
   * The rows of one code, newest first (history of successors).
   *
   * @param companyId company
   * @param code code
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<IncentiveCriteria> history(Long companyId, String code) {
    return criteria.findByCompanyIdAndCodeOrderByEffectiveFromDesc(companyId, code);
  }

  /**
   * Creates a criterion, pending authorisation.
   *
   * @param companyId company
   * @param code code (for example CPC2)
   * @param details attributes, products and period
   * @return the row
   */
  public IncentiveCriteria create(Long companyId, String code, Details details) {
    requireMaintainer();
    validate(companyId, details);
    requireNoOverlap(companyId, code, details, null);
    IncentiveCriteria saved = criteria.save(new IncentiveCriteria(companyId, code, details, null));
    audit.record(LABEL, saved.catalogReference(), AuditAction.CREATE, "Added " + details.name());
    return saved;
  }

  /**
   * Changes a criterion that is not yet active, pending authorisation.
   *
   * @param id row
   * @param details attributes
   * @return the row
   */
  public IncentiveCriteria update(Long id, Details details) {
    requireMaintainer();
    IncentiveCriteria row = get(id);
    validate(row.getCompanyId(), details);
    requireNoOverlap(row.getCompanyId(), row.getCode(), details, row.getId());
    row.update(details);
    audit.record(LABEL, row.catalogReference(), AuditAction.UPDATE, "Changed " + details.name());
    return row;
  }

  /**
   * Amends an active criterion: a successor row starting later is created, pending authorisation;
   * when it is authorised the active row ends the day before (PMADD07 "changes to active criteria
   * controlled", history kept).
   *
   * @param id active row
   * @param details the successor's attributes
   * @return the successor
   */
  public IncentiveCriteria amend(Long id, Details details) {
    requireMaintainer();
    IncentiveCriteria active = get(id);
    if (!active.isActive()) {
      throw new BusinessRuleException(
          "INCENTIVE_NOT_ACTIVE", "Only an active criterion is amended; change the pending row");
    }
    if (!details.effectiveFrom().isAfter(active.getEffectiveFrom())) {
      throw new BusinessRuleException(
          "INCENTIVE_PERIOD_INVALID",
          "The amendment must start after " + active.getEffectiveFrom());
    }
    validate(active.getCompanyId(), details);
    requireNoOverlap(active.getCompanyId(), active.getCode(), details, active.getId());
    IncentiveCriteria successor =
        criteria.save(
            new IncentiveCriteria(
                active.getCompanyId(), active.getCode(), details, active.getId()));
    audit.record(
        LABEL,
        successor.catalogReference(),
        AuditAction.CREATE,
        "Amendment of " + active.catalogReference());
    return successor;
  }

  /**
   * Deactivates a criterion (PMADD08): it ends on the given day and becomes INACTIVE; it stays in
   * the history.
   *
   * @param id row
   * @param lastDay last effective day, null for today
   * @return the row
   */
  public IncentiveCriteria deactivate(Long id, LocalDate lastDay) {
    requireMaintainer();
    IncentiveCriteria row = get(id);
    LocalDate end = lastDay != null ? lastDay : LocalDate.now(clock);
    if (row.isActive()) {
      row.endOn(end.isBefore(row.getEffectiveFrom()) ? row.getEffectiveFrom() : end);
    }
    row.deactivate();
    audit.record(LABEL, row.catalogReference(), AuditAction.DEACTIVATE, "Deactivated, ends " + end);
    publish(row, Change.DEACTIVATED);
    return row;
  }

  /**
   * The codes of the active criteria a transaction matches (booking, BRNB.107 / PMADD07).
   *
   * @param companyId company
   * @param facts product, cover type, segment, channel, insurer and date
   * @return distinct codes in alphabetical order, empty when none
   */
  @Transactional(readOnly = true)
  public List<String> matching(Long companyId, IncentiveFacts facts) {
    IncentiveCriteria.Facts f =
        new IncentiveCriteria.Facts(
            facts.productCode(),
            facts.coverTypeCode(),
            facts.segment(),
            facts.channel(),
            facts.insurerCode());
    return criteria.findByCompanyIdAndRecordStatus(companyId, RecordStatus.ACTIVE).stream()
        .filter(c -> c.appliesTo(f, facts.date()))
        .map(IncentiveCriteria::getCode)
        .distinct()
        .sorted()
        .toList();
  }

  @Override
  public void authorized(CatalogKind kind, AuthorizableEntity entity) {
    if (entity instanceof IncentiveCriteria row) {
      if (row.getSuccessorOf() == null) {
        publish(row, Change.ACTIVATED);
      } else {
        endPredecessor(row);
      }
    }
  }

  private void endPredecessor(IncentiveCriteria row) {
    IncentiveCriteria predecessor = get(row.getSuccessorOf());
    predecessor.endOn(row.getEffectiveFrom().minusDays(1));
    audit.record(
        LABEL,
        predecessor.catalogReference(),
        AuditAction.UPDATE,
        "Ended by successor " + row.catalogReference());
    publish(row, Change.AMENDED);
  }

  @Override
  public void deactivated(CatalogKind kind, AuthorizableEntity entity) {
    if (entity instanceof IncentiveCriteria row) {
      publish(row, Change.DEACTIVATED);
    }
  }

  private void publish(IncentiveCriteria row, Change change) {
    events.publishEvent(
        new IncentiveCriteriaChanged(
            row.getCompanyId(),
            row.getCode(),
            change,
            row.getEffectiveFrom(),
            row.getEffectiveTo()));
  }

  private void validate(Long companyId, Details details) {
    if (details.scopes().isEmpty()) {
      throw new BusinessRuleException(
          "INCENTIVE_PRODUCTS_REQUIRED", "Select at least one product of the products matrix");
    }
    lovs.requireValid("INCENTIVE_TYPE", details.incentiveType(), LocalDate.now(clock));
    details.scopes().forEach(scope -> requireActive(companyId, scope));
    if (details.ruleParams() != null && !isJson(details.ruleParams())) {
      throw new BusinessRuleException(
          "INCENTIVE_RULE_PARAMS_INVALID", "The rule parameters must be valid JSON");
    }
  }

  private void requireActive(Long companyId, IncentiveScope scope) {
    RiskProduct product = catalog.requireProduct(scope.productCode());
    if (!product.isSellable()) {
      throw new BusinessRuleException(
          "INCENTIVE_PRODUCT_NOT_ACTIVE",
          "Product " + scope.productCode() + " is not an active product of the matrix");
    }
    if (scope.insurerCode() != null && !scope.insurerCode().isBlank()) {
      insurers.requireUsableInsurer(companyId, scope.insurerCode());
    }
  }

  private boolean isJson(String text) {
    if (text.isBlank()) {
      return true;
    }
    try {
      json.readTree(text);
      return true;
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  private void requireNoOverlap(Long companyId, String code, Details details, Long ignoreId) {
    LocalDate from = details.effectiveFrom();
    LocalDate to = details.effectiveTo();
    boolean overlap =
        history(companyId, code).stream()
            .filter(r -> !Objects.equals(r.getId(), ignoreId))
            .filter(r -> r.getRecordStatus() != RecordStatus.INACTIVE)
            .anyMatch(
                r ->
                    (to == null || !r.getEffectiveFrom().isAfter(to))
                        && (r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(from)));
    if (overlap) {
      throw new BusinessRuleException(
          "INCENTIVE_PERIOD_OVERLAP", "Criterion " + code + " already covers part of this period");
    }
  }

  private void requireMaintainer() {
    if (!currentUser.hasAuthority(MAINTAIN)) {
      throw new AccessDeniedException("Incentive criteria are maintained with " + MAINTAIN);
    }
  }

  /**
   * What an invoice is matched on.
   *
   * @param productCode risk code
   * @param coverTypeCode cover type
   * @param segment market segment
   * @param channel source channel
   * @param insurerCode lead insurer party code
   * @param date booking date
   */
  public record IncentiveFacts(
      String productCode,
      String coverTypeCode,
      String segment,
      String channel,
      String insurerCode,
      LocalDate date) {}
}
