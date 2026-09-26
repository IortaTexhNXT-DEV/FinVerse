package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import java.util.Set;

/**
 * Port (SPI) through which the module that owns a record decides who may read or add its files. The
 * storage module never guesses: a file whose owner type has no resolver cannot be linked or stored
 * through the API. A module implements it as a bean in its own {@code service} package (the module
 * depends on {@code storage}, never the reverse).
 *
 * <pre>{@code
 * @Component
 * class ClaimFileAccess implements FileOwnerAccess {
 *   public Set<String> ownerTypes() { return Set.of("BrokerClaim"); }
 *   public boolean mayRead(FileOwner owner, String documentType) {
 *     return currentUser.hasAuthority("BCL_VIEW") && claims.visibleToCurrentUser(owner.entityId());
 *   }
 *   public boolean mayStore(FileOwner owner, String documentType) { ... }
 * }
 * }</pre>
 */
public interface FileOwnerAccess {

  /**
   * The owner entity types this resolver answers for (unique across resolvers).
   *
   * @return entity types, e.g. {@code BrokerClaim}
   */
  Set<String> ownerTypes();

  /**
   * Whether the current user may download a file of the record.
   *
   * @param owner owning record
   * @param documentType document type of the file (may be null)
   * @return true when allowed
   */
  boolean mayRead(FileOwner owner, String documentType);

  /**
   * Whether the current user may add a file to (or remove one from) the record.
   *
   * @param owner owning record
   * @param documentType document type of the file (may be null)
   * @return true when allowed
   */
  boolean mayStore(FileOwner owner, String documentType);
}
