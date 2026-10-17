package com.iortatechnxt.brokerverse.security.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Where a request body carries a company or a branch (DATA_SCOPE_DESIGN.md section 4): an accessor
 * {@code companyId()} / {@code getCompanyId()} (and {@code branchId()} / {@code getBranchId()})
 * returning a {@code Long}, on the body itself, on the elements of a list body, or on the elements
 * of a list component of a record body (bulk requests). The body advice checks these values; the
 * coverage tests use the same rules, so what the advice reads and what the build accepts agree.
 */
public final class DataScopeTargets {

  /** Request parameter and path variable of the company. */
  public static final String COMPANY = "companyId";

  /** Request parameter of a company branch. */
  public static final String BRANCH = "branchId";

  private static final ClassValue<Optional<Method>> COMPANY_ACCESSOR =
      new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
          return accessor(type, COMPANY);
        }
      };

  private static final ClassValue<Optional<Method>> BRANCH_ACCESSOR =
      new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
          return accessor(type, BRANCH);
        }
      };

  private DataScopeTargets() {}

  /**
   * Whether a body of the declared type carries a company the body advice reads.
   *
   * @param type declared body type (generic)
   * @return true when the advice checks the body
   */
  public static boolean carriesCompany(Type type) {
    Class<?> raw = rawClass(type);
    if (raw == null) {
      return false;
    }
    if (Collection.class.isAssignableFrom(raw)) {
      return carriesCompanyItself(rawClass(elementType(type)));
    }
    return carriesCompanyItself(raw) || raw.isRecord() && listComponentCarriesCompany(raw);
  }

  private static boolean listComponentCarriesCompany(Class<?> record) {
    for (RecordComponent c : record.getRecordComponents()) {
      if (Collection.class.isAssignableFrom(c.getType())
          && carriesCompanyItself(rawClass(elementType(c.getGenericType())))) {
        return true;
      }
    }
    return false;
  }

  /**
   * The company and branch values of a body as read: one pair per object carrying a company.
   *
   * @param body request body
   * @return companies with their branch, in body order
   */
  public static List<Target> companiesOf(Object body) {
    List<Target> found = new ArrayList<>();
    if (body instanceof Collection<?> items) {
      items.forEach(i -> addPair(i, found));
    } else if (body != null && !addPair(body, found) && body.getClass().isRecord()) {
      addFromListComponents(body, found);
    }
    return found;
  }

  /**
   * A company (and branch) named by a request body.
   *
   * @param companyId company, may be null
   * @param branchId branch, may be null
   */
  public record Target(Long companyId, Long branchId) {}

  private static void addFromListComponents(Object body, List<Target> found) {
    for (RecordComponent c : body.getClass().getRecordComponents()) {
      Method accessor = c.getAccessor();
      if (Collection.class.isAssignableFrom(c.getType())
          && accessor.trySetAccessible()
          && invoke(accessor, body) instanceof Collection<?> items) {
        items.forEach(i -> addPair(i, found));
      }
    }
  }

  private static boolean addPair(Object item, List<Target> found) {
    if (item == null) {
      return false;
    }
    Optional<Method> company = COMPANY_ACCESSOR.get(item.getClass());
    if (company.isEmpty()) {
      return false;
    }
    Long companyId = (Long) invoke(company.get(), item);
    Long branchId =
        BRANCH_ACCESSOR.get(item.getClass()).map(m -> (Long) invoke(m, item)).orElse(null);
    found.add(new Target(companyId, branchId));
    return true;
  }

  private static boolean carriesCompanyItself(Class<?> type) {
    return type != null && COMPANY_ACCESSOR.get(type).isPresent();
  }

  private static Optional<Method> accessor(Class<?> type, String name) {
    if (type.isPrimitive() || type.isArray() || type.getName().startsWith("java.")) {
      return Optional.empty();
    }
    String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    return Arrays.stream(type.getMethods())
        .filter(m -> m.getParameterCount() == 0)
        .filter(m -> m.getName().equals(name) || m.getName().equals(getter))
        .filter(m -> m.getReturnType() == Long.class && !Modifier.isStatic(m.getModifiers()))
        .filter(Method::trySetAccessible)
        .findFirst();
  }

  private static Object invoke(Method m, Object target) {
    try {
      return m.invoke(target);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Cannot read " + m.getName() + " of the request", ex);
    }
  }

  private static Type elementType(Type type) {
    return type instanceof ParameterizedType p && p.getActualTypeArguments().length == 1
        ? p.getActualTypeArguments()[0]
        : null;
  }

  private static Class<?> rawClass(Type type) {
    if (type instanceof Class<?> c) {
      return c;
    }
    if (type instanceof ParameterizedType p && p.getRawType() instanceof Class<?> c) {
      return c;
    }
    return null;
  }
}
