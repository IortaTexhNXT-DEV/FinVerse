package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.CatalogRecord;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maker-checker actions shared by every catalog record: authorize (a different user than the maker,
 * holding one of the kind's authorising permissions, {@link CatalogKind#authorizers()}) and
 * deactivate (a maintainer of the kind). Authorizing an insurer also authorizes its pending party
 * record, which the insurer screen created with it. Modules of the catalog that must react to an
 * authorization or deactivation (incentive criteria successors, PMADD07) implement {@link
 * CatalogRecordHook}.
 */
@Service
@Transactional
public class CatalogRecords {

  private final EntityManager entityManager;
  private final PartyService parties;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final List<CatalogRecordHook> hooks;

  /**
   * Creates the service.
   *
   * @param entityManager entity manager
   * @param parties parties (insurers)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param hooks reactions to authorizations and deactivations
   */
  public CatalogRecords(
      EntityManager entityManager,
      PartyService parties,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      List<CatalogRecordHook> hooks) {
    this.entityManager = entityManager;
    this.parties = parties;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.hooks = List.copyOf(hooks);
  }

  /**
   * One record of a kind.
   *
   * @param kind kind
   * @param id id
   * @return record
   */
  @Transactional(readOnly = true)
  public AuthorizableEntity get(CatalogKind kind, Long id) {
    AuthorizableEntity entity = entityManager.find(kind.type(), id);
    if (entity == null) {
      throw new ResourceNotFoundException(kind.label(), id);
    }
    return entity;
  }

  /**
   * Authorizes a new or changed record (checker, not the maker).
   *
   * @param kind kind
   * @param id id
   * @return the record
   */
  public AuthorizableEntity authorize(CatalogKind kind, Long id) {
    requireAny(kind.authorizers(), "authorize");
    AuthorizableEntity entity = get(kind, id);
    entity.authorize(currentUser.username(), clock.instant());
    if (entity instanceof InsurerProfile insurer) {
      Party party = parties.getByCode(insurer.getCompanyId(), insurer.getPartyCode());
      if (party.getRecordStatus() == RecordStatus.PENDING_AUTHORIZATION) {
        parties.authorize(party.getId());
      }
    }
    audit.record(kind.label(), reference(entity), AuditAction.AUTHORIZE, "Authorized");
    hooks.forEach(h -> h.authorized(kind, entity));
    return entity;
  }

  /**
   * Deactivates a record; it stays on existing accounts and in history.
   *
   * @param kind kind
   * @param id id
   * @return the record
   */
  public AuthorizableEntity deactivate(CatalogKind kind, Long id) {
    requireAny(kind.maintainers(), "deactivate");
    AuthorizableEntity entity = get(kind, id);
    entity.deactivate();
    audit.record(kind.label(), reference(entity), AuditAction.DEACTIVATE, "Deactivated");
    hooks.forEach(h -> h.deactivated(kind, entity));
    return entity;
  }

  private void requireAny(List<String> permissions, String action) {
    if (permissions.stream().noneMatch(currentUser::hasAuthority)) {
      throw new AccessDeniedException("Not allowed to " + action + " this kind of record");
    }
  }

  /**
   * Business reference of a record.
   *
   * @param entity record
   * @return reference
   */
  static String reference(AuthorizableEntity entity) {
    return entity instanceof CatalogRecord r
        ? r.catalogReference()
        : String.valueOf(entity.getId());
  }
}
