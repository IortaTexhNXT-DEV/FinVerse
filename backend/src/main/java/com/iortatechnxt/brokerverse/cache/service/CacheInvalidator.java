package com.iortatechnxt.brokerverse.cache.service;

import jakarta.persistence.EntityManagerFactory;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Clears caches when the entities they are built from change (every write path through JPA: the
 * services of the owning module, other modules, jobs, the bulk loader).
 *
 * <p>A change clears each affected cache at once (so a later read in the same transaction goes to
 * the database) and once more when the transaction ends, committed or rolled back (so a value read
 * inside the transaction never outlives it). With Redis the clear reaches every instance. Changes
 * made outside JPA (SQL scripts, a manual database fix) are not seen: flush the cache with {@code
 * POST /api/v1/admin/caches/{name}/clear} (runbook in PLATFORM_CACHE_AND_EVENTS.md).
 */
@Component
public class CacheInvalidator implements SmartInitializingSingleton {

  private static final Logger LOG = LoggerFactory.getLogger(CacheInvalidator.class);

  private final CacheManager caches;
  private final List<CacheSpec> specs;
  private final EntityManagerFactory entityManagerFactory;
  private final Map<Class<?>, Set<String>> namesByType = new ConcurrentHashMap<>();

  /**
   * Creates the invalidator.
   *
   * @param caches cache manager
   * @param specs cache declarations
   * @param entityManagerFactory JPA (Hibernate) entity manager factory
   */
  public CacheInvalidator(
      CacheManager caches, List<CacheSpec> specs, EntityManagerFactory entityManagerFactory) {
    this.caches = caches;
    this.specs = List.copyOf(specs);
    this.entityManagerFactory = entityManagerFactory;
  }

  @Override
  public void afterSingletonsInstantiated() {
    EventListenerRegistry registry =
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getEventEngine()
            .getListenerRegistry();
    EntityChangeListener listener = new EntityChangeListener();
    registry.appendListeners(EventType.POST_INSERT, listener);
    registry.appendListeners(EventType.POST_UPDATE, listener);
    registry.appendListeners(EventType.POST_DELETE, listener);
    registry.appendListeners(EventType.POST_COLLECTION_RECREATE, listener);
    registry.appendListeners(EventType.POST_COLLECTION_UPDATE, listener);
    registry.appendListeners(EventType.POST_COLLECTION_REMOVE, listener);
  }

  /**
   * Declared caches.
   *
   * @return declarations in registration order
   */
  public List<CacheSpec> specs() {
    return specs;
  }

  /**
   * Clears one cache now (support action).
   *
   * @param name cache name
   * @return true when the cache exists
   */
  public boolean clear(String name) {
    Cache cache = caches.getCache(name);
    if (cache == null) {
      return false;
    }
    cache.clear();
    return true;
  }

  /** Clears every declared cache now (support action). */
  public void clearAll() {
    specs.forEach(s -> clear(s.name()));
  }

  /**
   * Clears the caches built from an entity type, now and when the transaction ends.
   *
   * @param entityType changed entity type
   */
  public void entityChanged(Class<?> entityType) {
    Set<String> names = namesByType.computeIfAbsent(entityType, this::cachesOf);
    if (names.isEmpty()) {
      return;
    }
    Set<String> cleared =
        TransactionSynchronizationManager.isSynchronizationActive()
            ? clearedInTransaction()
            : new LinkedHashSet<>();
    for (String name : names) {
      if (cleared.add(name)) {
        clearQuietly(name);
      }
    }
  }

  private Set<String> cachesOf(Class<?> type) {
    Set<String> names = new LinkedHashSet<>();
    for (CacheSpec spec : specs) {
      if (spec.invalidatedBy().stream().anyMatch(t -> t.isAssignableFrom(type))) {
        names.add(spec.name());
      }
    }
    return Set.copyOf(names);
  }

  @SuppressWarnings("unchecked")
  private Set<String> clearedInTransaction() {
    Object bound = TransactionSynchronizationManager.getResource(this);
    if (bound != null) {
      return (Set<String>) bound;
    }
    Set<String> cleared = new LinkedHashSet<>();
    TransactionSynchronizationManager.bindResource(this, cleared);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            TransactionSynchronizationManager.unbindResourceIfPossible(CacheInvalidator.this);
            cleared.forEach(CacheInvalidator.this::clearQuietly);
          }
        });
    return cleared;
  }

  private void clearQuietly(String name) {
    try {
      clear(name);
    } catch (RuntimeException ex) {
      LOG.error("Cache {} could not be cleared after a change", name, ex);
    }
  }

  /** Hibernate listener forwarding every entity and collection change. */
  private final class EntityChangeListener
      implements PostInsertEventListener,
          PostUpdateEventListener,
          PostDeleteEventListener,
          PostCollectionRecreateEventListener,
          PostCollectionUpdateEventListener,
          PostCollectionRemoveEventListener {

    @Override
    public void onPostInsert(PostInsertEvent event) {
      entityChanged(event.getEntity().getClass());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
      entityChanged(event.getEntity().getClass());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
      entityChanged(event.getEntity().getClass());
    }

    @Override
    public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
      ownerChanged(event);
    }

    @Override
    public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
      ownerChanged(event);
    }

    @Override
    public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
      ownerChanged(event);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
      return false;
    }

    private void ownerChanged(AbstractCollectionEvent event) {
      Object owner = event.getAffectedOwnerOrNull();
      if (owner != null) {
        entityChanged(owner.getClass());
      }
    }
  }
}
