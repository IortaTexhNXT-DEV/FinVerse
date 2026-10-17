package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.audit.service.MasterChanges.FieldChange;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.domain.ChangeTracked;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

/**
 * Writes the master change log (G05): a Hibernate listener sees every insert and update of a
 * maker-checker master ({@link AuthorizableEntity}) and of a {@link ChangeTracked} master, whatever
 * the write path, and writes one row per changed field through the same connection just before the
 * transaction commits, so the log commits, or rolls back, with the change. When a change is
 * authorized, the checker and the time of the authorization are written on the rows of the record
 * still waiting for one.
 */
@Component
public class MasterChangeCapture implements SmartInitializingSingleton {

  static final String INSERT =
      "insert into master_change_log (entity_type, entity_id, change_kind, field_name, old_value,"
          + " new_value, maker, made_at) values (?, ?, ?, ?, ?, ?, ?, ?)";

  static final String AUTHORIZE =
      "update master_change_log set checker = ?, checked_at = ?"
          + " where entity_type = ? and entity_id = ? and checker is null";

  /** Field name of the row that records the creation of a record. */
  static final String RECORD_FIELD = "record";

  private final EntityManagerFactory entityManagerFactory;
  private final Clock clock;

  /**
   * Creates the capture.
   *
   * @param entityManagerFactory JPA (Hibernate) entity manager factory
   * @param clock clock
   */
  public MasterChangeCapture(EntityManagerFactory entityManagerFactory, Clock clock) {
    this.entityManagerFactory = entityManagerFactory;
    this.clock = clock;
  }

  @Override
  public void afterSingletonsInstantiated() {
    EventListenerRegistry registry =
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getEventEngine()
            .getListenerRegistry();
    Listener listener = new Listener();
    registry.appendListeners(EventType.POST_INSERT, listener);
    registry.appendListeners(EventType.POST_UPDATE, listener);
  }

  /**
   * Whether the changes of an entity are logged.
   *
   * @param entity entity
   * @return true for maker-checker and change-tracked masters
   */
  static boolean tracked(Object entity) {
    return entity instanceof AuthorizableEntity || entity instanceof ChangeTracked;
  }

  /**
   * Record type of an entity in the log: its class name.
   *
   * @param entity entity
   * @return record type
   */
  static String entityType(Object entity) {
    return entity.getClass().getSimpleName();
  }

  private static String maker(BaseEntity entity) {
    if (entity instanceof AuthorizableEntity a) {
      return a.getMaker();
    }
    return entity.getUpdatedBy() != null ? entity.getUpdatedBy() : entity.getCreatedBy();
  }

  private static boolean[] loggable(EntityPersister persister) {
    Type[] types = persister.getPropertyTypes();
    boolean[] loggable = new boolean[types.length];
    for (int i = 0; i < types.length; i++) {
      Type t = types[i];
      loggable[i] = !t.isAssociationType() && !t.isCollectionType() && !t.isComponentType();
    }
    return loggable;
  }

  private void write(EventSource session, BaseEntity entity, String kind, List<FieldChange> rows) {
    if (rows.isEmpty()) {
      return;
    }
    String type = entityType(entity);
    Long id = entity.getId();
    String maker = maker(entity);
    Timestamp at = Timestamp.from(clock.instant());
    session
        .getActionQueue()
        .registerProcess(
            (BeforeTransactionCompletionProcess)
                s ->
                    s.doWork(
                        connection -> {
                          JdbcTemplate jdbc =
                              new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                          for (FieldChange c : rows) {
                            jdbc.update(
                                INSERT,
                                type,
                                id,
                                kind,
                                c.field(),
                                c.oldValue(),
                                c.newValue(),
                                maker,
                                at);
                          }
                        }));
  }

  private void authorize(EventSource session, AuthorizableEntity entity) {
    String type = entityType(entity);
    Long id = entity.getId();
    String checker = entity.getAuthorizedBy();
    Instant when = entity.getAuthorizedAt() == null ? clock.instant() : entity.getAuthorizedAt();
    session
        .getActionQueue()
        .registerProcess(
            (BeforeTransactionCompletionProcess)
                s ->
                    s.doWork(
                        connection ->
                            new JdbcTemplate(new SingleConnectionDataSource(connection, true))
                                .update(AUTHORIZE, checker, Timestamp.from(when), type, id)));
  }

  /** Hibernate listener of the logged masters. */
  private final class Listener implements PostInsertEventListener, PostUpdateEventListener {

    @Override
    public void onPostInsert(PostInsertEvent event) {
      if (tracked(event.getEntity()) && event.getEntity() instanceof BaseEntity entity) {
        write(
            event.getSession(),
            entity,
            "CREATE",
            List.of(new FieldChange(RECORD_FIELD, null, "Created")));
      }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
      if (!tracked(event.getEntity())
          || !(event.getEntity() instanceof BaseEntity entity)
          || event.getOldState() == null) {
        return;
      }
      String[] names = event.getPersister().getPropertyNames();
      Object[] before = event.getOldState();
      Object[] after = event.getState();
      write(
          event.getSession(),
          entity,
          "UPDATE",
          MasterChanges.diff(names, loggable(event.getPersister()), before, after));
      if (entity instanceof AuthorizableEntity a
          && MasterChanges.authorizes(names, before, after)) {
        authorize(event.getSession(), a);
      }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
      return false;
    }
  }
}
