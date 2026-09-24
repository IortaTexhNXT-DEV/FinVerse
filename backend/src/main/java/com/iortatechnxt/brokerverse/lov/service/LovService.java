package com.iortatechnxt.brokerverse.lov.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.domain.LovDetails;
import com.iortatechnxt.brokerverse.lov.domain.LovType;
import com.iortatechnxt.brokerverse.lov.domain.LovTypeRepository;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.domain.LovValueRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists of values: lookup for validations and screens, maintenance under maker-checker (BRNB.083).
 * Business modules validate coded fields with {@link #requireValid}.
 */
@Service
@Transactional
public class LovService {

  private static final String ENTITY = "LovValue";

  private final LovTypeRepository types;
  private final LovValueRepository values;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param types list types
   * @param values list values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public LovService(
      LovTypeRepository types,
      LovValueRepository values,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.types = types;
    this.values = values;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * All list types.
   *
   * @return types by name
   */
  @Transactional(readOnly = true)
  public List<LovType> types() {
    return types.findAllByOrderByName();
  }

  /**
   * Every value of a list, whatever its status, for maintenance.
   *
   * @param typeCode list
   * @return values in display order
   */
  @Transactional(readOnly = true)
  public List<LovValue> values(String typeCode) {
    requireType(typeCode);
    return values.findByTypeCodeOrderBySortOrderAscLabelAsc(typeCode);
  }

  /**
   * Values usable on a date, for pick lists.
   *
   * @param typeCode list
   * @param date business date
   * @return usable values in display order
   */
  @Transactional(readOnly = true)
  public List<LovValue> activeValues(String typeCode, LocalDate date) {
    return values(typeCode).stream().filter(v -> v.isUsableOn(date)).toList();
  }

  /**
   * Validates a coded field: the code must be usable on the date.
   *
   * @param typeCode list
   * @param code code entered
   * @param date business date
   * @return the value (for its label)
   */
  @Transactional(readOnly = true)
  public LovValue requireValid(String typeCode, String code, LocalDate date) {
    return values
        .findByTypeCodeAndCode(typeCode, code)
        .filter(v -> v.isUsableOn(date))
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "LOV_VALUE_INVALID",
                    "'" + code + "' is not a valid value of " + typeCode + " on " + date));
  }

  /**
   * Validates an optional coded field (null or blank is accepted).
   *
   * @param typeCode list
   * @param code code entered, may be blank
   * @param date business date
   */
  @Transactional(readOnly = true)
  public void validateOptional(String typeCode, String code, LocalDate date) {
    if (code != null && !code.isBlank()) {
      requireValid(typeCode, code, date);
    }
  }

  /**
   * Label of a code, or the code itself when unknown (reports).
   *
   * @param typeCode list
   * @param code code
   * @return label
   */
  @Transactional(readOnly = true)
  public String label(String typeCode, String code) {
    if (code == null) {
      return null;
    }
    return values.findByTypeCodeAndCode(typeCode, code).map(LovValue::getLabel).orElse(code);
  }

  /**
   * Adds a value, pending authorization.
   *
   * @param typeCode list
   * @param code code
   * @param details label, order, parent and effectivity
   * @return the value
   */
  public LovValue create(String typeCode, String code, LovDetails details) {
    requireMaintainable(typeCode);
    if (values.findByTypeCodeAndCode(typeCode, code).isPresent()) {
      throw new DuplicateResourceException(typeCode, code);
    }
    LovValue saved = values.save(new LovValue(typeCode, code, details));
    audit.record(ENTITY, key(saved), AuditAction.CREATE, "Added '" + details.label() + "'");
    return saved;
  }

  /**
   * Changes a value (label, order or effectivity); the change must be authorized again.
   *
   * @param id value id
   * @param details new attributes
   * @return the value
   */
  public LovValue update(Long id, LovDetails details) {
    LovValue value = get(id);
    requireMaintainable(value.getTypeCode());
    value.update(details);
    audit.record(
        ENTITY,
        key(value),
        AuditAction.UPDATE,
        "Changed to '"
            + details.label()
            + "', effective "
            + details.effectiveFrom()
            + (details.effectiveTo() == null ? "" : " to " + details.effectiveTo()));
    return value;
  }

  /**
   * Authorizes a new or changed value (checker, not the maker).
   *
   * @param id value id
   * @return the value
   */
  public LovValue authorize(Long id) {
    LovValue value = get(id);
    value.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, key(value), AuditAction.AUTHORIZE, "Authorized");
    return value;
  }

  /**
   * Deactivates a value; it stays visible in history and on existing records.
   *
   * @param id value id
   * @return the value
   */
  public LovValue deactivate(Long id) {
    LovValue value = get(id);
    requireMaintainable(value.getTypeCode());
    value.deactivate();
    audit.record(ENTITY, key(value), AuditAction.DEACTIVATE, "Deactivated");
    return value;
  }

  /**
   * One value.
   *
   * @param id id
   * @return value
   */
  @Transactional(readOnly = true)
  public LovValue get(Long id) {
    return values.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private LovType requireType(String typeCode) {
    return types
        .findByCode(typeCode)
        .orElseThrow(() -> new ResourceNotFoundException("LovType", typeCode));
  }

  private void requireMaintainable(String typeCode) {
    if (!requireType(typeCode).isMaintainable()) {
      throw new BusinessRuleException(
          "LOV_NOT_MAINTAINABLE", "The list " + typeCode + " is maintained by the system");
    }
  }

  private static String key(LovValue value) {
    return value.getTypeCode() + ":" + value.getCode();
  }
}
