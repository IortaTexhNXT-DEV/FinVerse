package com.iortatechnxt.brokerverse.migration.common.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Who may read or add the files of the migration records: extract files (operators and viewers of
 * the console), sign-off evidence and cutover task evidence (console users). Staging files are
 * reachable only by migration roles (DATA_MIGRATION_DESIGN section 18.3).
 */
@Component
public class MigrationFileAccess implements FileOwnerAccess {

  private final CurrentUser currentUser;

  /**
   * Creates the resolver.
   *
   * @param currentUser current user
   */
  public MigrationFileAccess(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  public Set<String> ownerTypes() {
    return Set.of(
        MigrationCodes.ENTITY_EXTRACT,
        MigrationCodes.ENTITY_SIGNOFF,
        MigrationCodes.ENTITY_CUTOVER);
  }

  @Override
  public boolean mayRead(FileOwner owner, String documentType) {
    if (MigrationCodes.ENTITY_EXTRACT.equals(owner.entityType())) {
      return currentUser.hasAuthority("MIG_INTAKE") || currentUser.hasAuthority("MIG_LOAD_RUN");
    }
    return currentUser.hasAuthority("MIG_VIEW");
  }

  @Override
  public boolean mayStore(FileOwner owner, String documentType) {
    if (MigrationCodes.ENTITY_EXTRACT.equals(owner.entityType())) {
      return currentUser.hasAuthority("MIG_INTAKE");
    }
    if (MigrationCodes.ENTITY_CUTOVER.equals(owner.entityType())) {
      return currentUser.hasAuthority("MIG_CUTOVER_MANAGE");
    }
    return currentUser.hasAuthority("MIG_SIGNOFF")
        || currentUser.hasAuthority("MIG_RECON_SIGNOFF")
        || currentUser.hasAuthority("MIG_DQ_RESOLVE")
        || currentUser.hasAuthority("MIG_LOAD_APPROVE");
  }
}
