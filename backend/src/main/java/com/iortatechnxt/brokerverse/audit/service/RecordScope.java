package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.audit.service.RecordOwnership.Owner;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.DataScope;
import com.iortatechnxt.brokerverse.common.security.RecordOwnerLookup;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refuses a record addressed by its id when it belongs to a company or branch outside the data
 * scope of the signed-in user, with HTTP 404 as if it did not exist (the record scope check of
 * DATA_SCOPE_DESIGN.md section 4.1, the same answer as the change history of G05). A user with all
 * companies is not checked; a record that does not exist is left to the endpoint. The check reads
 * in a transaction of its own, so that it never flushes the pending changes of the caller's
 * transaction (and their auditing) before the endpoint runs.
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
public class RecordScope {

  private final RecordOwnership ownership;
  private final DataScope dataScope;
  private final Map<String, RecordOwnerLookup> lookups;

  /**
   * Creates the check.
   *
   * @param ownership owner resolver
   * @param dataScope data scope of the signed-in user
   * @param lookups owners of the records kept outside the entity model
   */
  public RecordScope(
      RecordOwnership ownership, DataScope dataScope, List<RecordOwnerLookup> lookups) {
    this.ownership = ownership;
    this.dataScope = dataScope;
    this.lookups =
        lookups.stream()
            .collect(
                Collectors.toUnmodifiableMap(RecordOwnerLookup::recordType, Function.identity()));
  }

  /**
   * Whether a record type kept outside the entity model has a lookup.
   *
   * @param recordType record type
   * @return true when known
   */
  public boolean knowsType(String recordType) {
    return lookups.containsKey(recordType);
  }

  /**
   * Refuses a record kept outside the entity model, or a stored record named by its entity name,
   * outside the data scope.
   *
   * @param recordType record type of a {@link RecordOwnerLookup}, or an entity name
   * @param id record id, null is not checked
   */
  public void require(String recordType, Long id) {
    UserDataScope scope = restrictedScope(id);
    if (scope != null) {
      RecordOwnerLookup lookup = lookups.get(recordType);
      Optional<Owner> owner =
          lookup == null
              ? ownership.ownerOf(recordType, id)
              : lookup.companyOf(id).map(company -> new Owner(company, null));
      refuseOutside(scope, owner, recordType, id);
    }
  }

  /**
   * Refuses a record outside the data scope.
   *
   * @param type entity class of the record
   * @param id record id, null is not checked
   */
  public void require(Class<?> type, Long id) {
    UserDataScope scope = restrictedScope(id);
    if (scope != null) {
      refuseOutside(scope, ownership.ownerOf(type, id), type.getSimpleName(), id);
    }
  }

  /** The scope to check a record against, null when nothing is checked. */
  private UserDataScope restrictedScope(Long id) {
    UserDataScope scope = id == null ? null : dataScope.allowed();
    return scope == null || scope.allCompanies() ? null : scope;
  }

  private static void refuseOutside(
      UserDataScope scope, Optional<Owner> owner, String recordType, Long id) {
    if (owner.isPresent() && !RecordOwnership.visible(scope, owner)) {
      throw new ResourceNotFoundException(recordType, id);
    }
  }
}
