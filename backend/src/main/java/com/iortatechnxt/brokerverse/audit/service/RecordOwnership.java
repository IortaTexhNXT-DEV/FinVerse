package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.common.security.OwnedBy;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The company and branch that own a logged master record (G05), read from the record itself: its
 * {@code getCompanyId()} / {@code getBranchId()} accessors, or its {@code getCompany()} association
 * (a branch), or the record id for a company, or, for a record marked {@link OwnedBy}, the owner of
 * its parent record, or, for a record linked to another by {@code getEntityType()} / {@code
 * getEntityId()} (an attachment), the owner of the linked record. A record without any of these is
 * programme-wide.
 */
@Service
@Transactional(readOnly = true)
public class RecordOwnership {

  /** Parent records followed at most, against a loop in the declarations. */
  private static final int MAX_PARENTS = 4;

  /** Records with one text key read at most (a key is unique within a company). */
  private static final int MAX_KEY_MATCHES = 50;

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
    return ownerOf(type, entityId);
  }

  /**
   * Resolves the owner of a record of a known type.
   *
   * @param type entity class
   * @param entityId record id
   * @return owner, empty when the record does not exist
   */
  public Optional<Owner> ownerOf(Class<?> type, Long entityId) {
    return resolve(type, entityId, 0);
  }

  /**
   * Resolves the owners of the records of a known type that carry a text business key (the invoice
   * number, the account reference, the batch number). Several companies may use the same key; each
   * record found gives its owner.
   *
   * @param type entity class
   * @param property the entity property holding the key
   * @param key key value
   * @return owners of the records with the key, empty when there is none
   */
  public List<Owner> ownersByKey(Class<?> type, String property, String key) {
    if (key == null || key.isBlank()) {
      return List.of();
    }
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Object> query = cb.createQuery(Object.class);
    Root<?> root = query.from(type);
    query.select(root).where(cb.equal(root.get(property), key.trim()));
    PersistenceUnitUtil ids = em.getEntityManagerFactory().getPersistenceUnitUtil();
    return em.createQuery(query).setMaxResults(MAX_KEY_MATCHES).getResultList().stream()
        .map(row -> ownerOfRow(type, ids.getIdentifier(row), row, 0))
        .toList();
  }

  /**
   * Resolves the owners of the records that carry a text business key, by entity name.
   *
   * @param entityType record type (entity class simple name)
   * @param property the entity property holding the key
   * @param key key value
   * @return owners, empty when the type is unknown or no record carries the key
   */
  public List<Owner> ownersByKey(String entityType, String property, String key) {
    Class<?> type = types.get(entityType);
    return type == null ? List.of() : ownersByKey(type, property, key);
  }

  /**
   * Whether a record type is a stored entity with a property.
   *
   * @param entityType entity class simple name
   * @param property property holding a text business key
   * @return true when known
   */
  public boolean knowsEntity(String entityType, String property) {
    Class<?> type = types.get(entityType);
    return type != null
        && em.getMetamodel().entity(type).getAttributes().stream()
            .anyMatch(a -> a.getName().equals(property));
  }

  /**
   * Whether a reader of the given scope may see a record of the given owner: a programme-wide
   * record is visible to everyone, a company or branch record to a reader whose scope holds it. An
   * unknown record is not visible.
   *
   * @param scope data scope of the reader
   * @param owner owner of the record, empty when unknown
   * @return true when visible
   */
  public static boolean visible(UserDataScope scope, Optional<Owner> owner) {
    if (scope.allCompanies()) {
      return true;
    }
    if (owner.isEmpty()) {
      return false;
    }
    Owner o = owner.get();
    if (o.company() == null) {
      return true;
    }
    return o.branch() == null
        ? scope.allowsCompany(o.company())
        : scope.allowsBranch(o.company(), o.branch());
  }

  private Optional<Owner> resolve(Class<?> type, Object entityId, int depth) {
    if (entityId == null) {
      return Optional.empty();
    }
    Object row = em.find(type, entityId);
    if (row == null) {
      return Optional.empty();
    }
    return Optional.of(ownerOfRow(type, entityId, row, depth));
  }

  private Owner ownerOfRow(Class<?> type, Object entityId, Object row, int depth) {
    Owner own = owner(type.getSimpleName(), entityId instanceof Long l ? l : null, row);
    if (own.company() != null || depth >= MAX_PARENTS) {
      return own;
    }
    OwnedBy parent = type.getAnnotation(OwnedBy.class);
    if (parent == null) {
      return linkedOwner(row, depth).orElse(own);
    }
    String getter =
        "get" + Character.toUpperCase(parent.key().charAt(0)) + parent.key().substring(1);
    Object key = valueOf(row, getter);
    if (key != null && parent.value().isInstance(key)) {
      Object parentId = valueOf(key, "getId");
      return ownerOfRow(parent.value(), parentId, key, depth + 1);
    }
    return resolve(parent.value(), key, depth + 1).orElse(Owner.NONE);
  }

  private Optional<Owner> linkedOwner(Object row, int depth) {
    if (!(valueOf(row, "getEntityType") instanceof String linkedType)
        || !(valueOf(row, "getEntityId") instanceof String linkedId)
        || !types.containsKey(linkedType)) {
      return Optional.empty();
    }
    try {
      return resolve(types.get(linkedType), Long.valueOf(linkedId.trim()), depth + 1);
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
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
