package com.iortatechnxt.brokerverse;

import com.iortatechnxt.brokerverse.common.security.OwnedBy;
import jakarta.persistence.Entity;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;

/** The path variables and records the record scope rules of {@link ArchitectureTest} read. */
final class RecordScopeRuleSupport {

  private RecordScopeRuleSupport() {}

  /**
   * Text path variables that select a kind, an action, a format or a programme-wide code (a list of
   * values, a catalog insurer, a user name, a content hash), not a record of a company.
   */
  static final Set<String> NOT_RECORD_KEYS =
      Set.of(
          "type",
          "kind",
          "action",
          "format",
          "part",
          "step",
          "fileType",
          "system",
          "source",
          "name",
          "sha",
          "sha256",
          "sessionId",
          "key",
          "month",
          "eventCode",
          "workflowCode",
          "setCode",
          "typeCode",
          "unitCode",
          "objectCode",
          "username",
          "code",
          "insurerCode",
          "insurer",
          "id");

  /**
   * The text path variables of an endpoint that address a record of a company by its business key
   * (invoice number, account reference, batch number, renewal reference).
   *
   * @param method endpoint
   * @return path variable names
   */
  static List<String> textKeys(Method method) {
    List<String> keys = new ArrayList<>();
    for (Parameter p : method.getParameters()) {
      PathVariable variable = p.getAnnotation(PathVariable.class);
      if (variable == null || p.getType() != String.class) {
        continue;
      }
      String name =
          !variable.value().isEmpty()
              ? variable.value()
              : !variable.name().isEmpty() ? variable.name() : p.getName();
      if (!NOT_RECORD_KEYS.contains(name)) {
        keys.add(name);
      }
    }
    return keys;
  }

  /**
   * The numeric path variables of an endpoint that address a record (the company itself is checked
   * by the data scope interceptor).
   *
   * @param method endpoint
   * @return path variable names
   */
  static List<String> recordIds(Method method) {
    List<String> ids = new ArrayList<>();
    for (Parameter p : method.getParameters()) {
      PathVariable variable = p.getAnnotation(PathVariable.class);
      if (variable == null || p.getType() != Long.class && p.getType() != long.class) {
        continue;
      }
      String name =
          !variable.value().isEmpty()
              ? variable.value()
              : !variable.name().isEmpty() ? variable.name() : p.getName();
      if (!"companyId".equals(name)) {
        ids.add(name);
      }
    }
    return ids;
  }

  /**
   * Whether a stored record belongs to a company: it carries the company, it is a company, it names
   * its parent ({@code OwnedBy}), or it links to another record (an attachment).
   *
   * @param type entity class
   * @return true when the record scope check can resolve its company
   */
  static boolean ownedByACompany(Class<?> type) {
    if (!type.isAnnotationPresent(Entity.class)) {
      return false;
    }
    boolean ownCompany =
        "Company".equals(type.getSimpleName())
            || hasAccessor(type, "getCompanyId")
            || hasAccessor(type, "getCompany");
    boolean linked = hasAccessor(type, "getEntityType") && hasAccessor(type, "getEntityId");
    return ownCompany || linked || type.isAnnotationPresent(OwnedBy.class);
  }

  static boolean hasProperty(Class<?> type, String property) {
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      try {
        c.getDeclaredField(property);
        return true;
      } catch (NoSuchFieldException ex) {
        // look in the superclass
      }
    }
    return false;
  }

  static boolean hasAccessor(Class<?> type, String name) {
    try {
      return type.getMethod(name).getParameterCount() == 0;
    } catch (NoSuchMethodException ex) {
      return false;
    }
  }
}
