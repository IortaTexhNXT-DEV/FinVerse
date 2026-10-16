package com.iortatechnxt.brokerverse.audit.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The company and branch that own a logged master record (G05), read from the record itself: its
 * {@code getCompanyId()} / {@code getBranchId()} accessors, or its {@code getCompany()} association
 * (a branch), or the record id for a company. A record without any of these is programme-wide.
 */
@Service
@Transactional(readOnly = true)
public class RecordOwnership {

  /** Record type of a company in the log. */
  static final String COMPANY_TYPE = "Company";

  @PersistenceContext private EntityManager em;

  private final Map<String, Class<?>> types;

  /**
   * Creates the resolver.
   *
   * @param entityManagerFactory JPA entity manager factory (the known record types)
   */
  public RecordOwnership(EntityManagerFactory entityManagerFactory) {
    this.types =
        entityManagerFactory.getMetamodel().getEntities().stream()
            .map(EntityType::getJavaType)
            .collect(
                Collectors.toUnmodifiableMap(
                    Class::getSimpleName, Function.identity(), (a, b) -> a));
  }

  /**
   * The owner of a record.
   *
   * @param company owning company, null for a programme-wide record
   * @param branch owning branch, null when the record is not held by a branch
   */
  public record Owner(Long company, Long branch) {

    /** A programme-wide record. */
    public static final Owner NONE = new Owner(null, null);
  }

  /**
   * Resolves the owner of a record.
   *
   * @param entityType record type as logged (entity class simple name)
   * @param entityId record id
   * @return owner, empty when the record type is unknown or the record no longer exists
   */
  public Optional<Owner> ownerOf(String entityType, Long entityId) {
    Class<?> type = types.get(entityType);
    if (type == null || entityId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(em.find(type, entityId))
        .map(row -> owner(entityType, entityId, row));
  }

  private static Owner owner(String entityType, Long entityId, Object row) {
    if (COMPANY_TYPE.equals(entityType)) {
      return new Owner(entityId, null);
    }
    Long company = longOf(row, "getCompanyId");
    Long branch = longOf(row, "getBranchId");
    if (company == null) {
      Object parent = valueOf(row, "getCompany");
      company = parent == null ? null : longOf(parent, "getId");
      if (company != null && "Branch".equals(entityType)) {
        branch = entityId;
      }
    }
    return new Owner(company, branch);
  }

  private static Long longOf(Object target, String accessor) {
    return valueOf(target, accessor) instanceof Long l ? l : null;
  }

  private static Object valueOf(Object target, String accessor) {
    try {
      Method m = target.getClass().getMethod(accessor);
      return m.getParameterCount() == 0 ? m.invoke(target) : null;
    } catch (NoSuchMethodException ex) {
      return null;
    } catch (IllegalAccessException | InvocationTargetException ex) {
      throw new IllegalStateException("Cannot read the owner of a " + target.getClass(), ex);
    }
  }
}
