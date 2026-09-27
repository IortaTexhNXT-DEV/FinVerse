package com.iortatechnxt.brokerverse.dimension.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionValue;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionValueRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintains and validates financial dimension values. */
@Service
@Transactional
public class DimensionService {

  private final DimensionValueRepository repository;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param repository repository
   * @param audit audit trail
   */
  public DimensionService(DimensionValueRepository repository, AuditTrailService audit) {
    this.repository = repository;
    this.audit = audit;
  }

  /**
   * Lists values of a dimension.
   *
   * @param companyId company
   * @param type type
   * @return values
   */
  @Transactional(readOnly = true)
  public List<DimensionValue> list(Long companyId, DimensionType type) {
    return repository.findByCompanyIdAndTypeOrderByCode(companyId, type);
  }

  /**
   * Creates a dimension value.
   *
   * @param companyId company
   * @param type type
   * @param code code
   * @param name name
   * @return value
   */
  public DimensionValue create(Long companyId, DimensionType type, String code, String name) {
    if (repository.findByCompanyIdAndTypeAndCode(companyId, type, code).isPresent()) {
      throw new DuplicateResourceException(type.name(), code);
    }
    DimensionValue saved = repository.save(new DimensionValue(companyId, type, code, name));
    audit.record("DimensionValue", type + ":" + code, AuditAction.CREATE, "Created " + name);
    return saved;
  }

  /**
   * Activates or deactivates a value.
   *
   * @param id id
   * @param active new state
   * @return value
   */
  public DimensionValue setActive(Long id, boolean active) {
    DimensionValue value =
        repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dimension", id));
    value.setActive(active);
    audit.record(
        "DimensionValue",
        value.getType() + ":" + value.getCode(),
        active ? AuditAction.UPDATE : AuditAction.DEACTIVATE,
        active ? "Activated" : "Deactivated");
    return value;
  }

  /**
   * Validates that an optional code refers to an active value.
   *
   * @param companyId company
   * @param type type
   * @param code code, may be null or blank
   */
  @Transactional(readOnly = true)
  public void validateOptional(Long companyId, DimensionType type, String code) {
    if (code == null || code.isBlank()) {
      return;
    }
    boolean valid =
        repository
            .findByCompanyIdAndTypeAndCode(companyId, type, code)
            .map(DimensionValue::isActive)
            .orElse(false);
    if (!valid) {
      throw new BusinessRuleException(
          "INVALID_DIMENSION", "Unknown or inactive " + type + " '" + code + "'");
    }
  }
}
