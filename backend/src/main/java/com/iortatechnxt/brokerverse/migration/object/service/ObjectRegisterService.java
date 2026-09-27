package com.iortatechnxt.brokerverse.migration.object.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject.ObjectData;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObjectRepository;
import com.iortatechnxt.brokerverse.migration.object.domain.MigrationClass;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The data object register (BRID 1.1a; FR-DM-001): objects with their source systems, target,
 * owners, the four criteria, the proposed class, dependencies and load order. An object is never
 * deleted; the load order follows the objects it depends on and dependencies have no cycle.
 */
@Service
@Transactional
public class ObjectRegisterService {

  private static final int MAX_RATIONALE = 2000;

  private final MigDataObjectRepository objects;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param objects register
   * @param audit audit trail
   */
  public ObjectRegisterService(MigDataObjectRepository objects, AuditTrailService audit) {
    this.objects = objects;
    this.audit = audit;
  }

  /**
   * The register in load order.
   *
   * @return objects
   */
  @Transactional(readOnly = true)
  public List<MigDataObject> list() {
    return objects.findAllByOrderByLoadOrderAscCodeAsc();
  }

  /**
   * An object.
   *
   * @param code object code
   * @return object
   */
  @Transactional(readOnly = true)
  public MigDataObject get(String code) {
    return objects
        .findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_OBJECT, code));
  }

  /**
   * Adds an object, PROPOSED.
   *
   * @param code object code (letters and digits)
   * @param data object data
   * @return object
   */
  public MigDataObject create(String code, ObjectData data) {
    String key = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    if (!key.matches("[A-Z0-9]{2,10}")) {
      throw new BusinessRuleException(
          "MIG_OBJECT_CODE", "The object code is 2 to 10 letters and digits");
    }
    if (objects.findByCode(key).isPresent()) {
      throw new DuplicateResourceException(MigrationCodes.ENTITY_OBJECT, key);
    }
    validate(key, data);
    MigDataObject saved = objects.save(new MigDataObject(key, data));
    audit.record(
        MigrationCodes.ENTITY_OBJECT,
        key,
        AuditAction.CREATE,
        "Proposed " + data.proposedClass() + ": " + data.name());
    return saved;
  }

  /**
   * Changes an object; the decided class stays until a new decision is approved.
   *
   * @param code object code
   * @param data object data
   * @return object
   */
  public MigDataObject update(String code, ObjectData data) {
    MigDataObject object = get(code);
    validate(code, data);
    String before = describe(object);
    object.apply(data);
    audit.record(
        MigrationCodes.ENTITY_OBJECT,
        code,
        AuditAction.UPDATE,
        "Changed from [" + before + "] to [" + describe(object) + "]");
    return object;
  }

  private static String describe(MigDataObject o) {
    return o.getName()
        + "; class "
        + o.getProposedClass()
        + "; order "
        + o.getLoadOrder()
        + "; depends on "
        + (o.getDependsOn() == null ? "-" : o.getDependsOn())
        + "; owner "
        + o.getBusinessOwner()
        + "; steward "
        + o.getDataSteward();
  }

  private void validate(String code, ObjectData data) {
    if (data.rationale() == null || data.rationale().isBlank()) {
      throw new BusinessRuleException(
          "MIG_RATIONALE_REQUIRED", "Enter the rationale for the proposed class");
    }
    if (data.rationale().length() > MAX_RATIONALE) {
      throw new BusinessRuleException(
          "MIG_RATIONALE_LENGTH", "The rationale is limited to 2,000 characters");
    }
    if (data.proposedClass() == MigrationClass.CONDITIONAL
        && (data.conditionText() == null || data.conditionText().isBlank())) {
      throw new BusinessRuleException(
          "MIG_CONDITION_REQUIRED", "Enter the condition of a conditional object");
    }
    Map<String, MigDataObject> all =
        objects.findAll().stream()
            .collect(Collectors.toMap(MigDataObject::getCode, Function.identity()));
    List<String> deps = dependencies(data.dependsOn());
    for (String dep : deps) {
      MigDataObject parent = all.get(dep);
      if (parent == null) {
        throw new BusinessRuleException("MIG_OBJECT_UNKNOWN", "Object " + dep + " does not exist");
      }
      if (data.loadOrder() <= parent.getLoadOrder()) {
        throw new BusinessRuleException(
            "MIG_LOAD_ORDER", "The load order must follow the objects this object depends on");
      }
    }
    requireNoCycle(code, deps, all);
  }

  private static void requireNoCycle(
      String code, List<String> deps, Map<String, MigDataObject> all) {
    Deque<String> pending = new ArrayDeque<>(deps);
    Set<String> seen = new HashSet<>();
    while (!pending.isEmpty()) {
      String next = pending.pop();
      if (next.equals(code)) {
        throw new BusinessRuleException(
            "MIG_DEPENDENCY_CYCLE", "Object " + code + " cannot depend on itself through " + deps);
      }
      MigDataObject o = all.get(next);
      if (seen.add(next) && o != null) {
        pending.addAll(o.dependencies());
      }
    }
  }

  private static List<String> dependencies(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    return Arrays.stream(csv.split(","))
        .map(s -> s.trim().toUpperCase(Locale.ROOT))
        .filter(s -> !s.isEmpty())
        .toList();
  }
}
