package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import java.util.List;
import java.util.Set;

/**
 * A {@link FileOwnerAccess} for owner types whose files the application produces itself and that
 * are opened by the holders of any of a list of permissions (the permissions of the owning module's
 * screen). Nobody adds files through the file API.
 */
public class PermissionFileAccess implements FileOwnerAccess {

  private final Set<String> ownerTypes;
  private final List<String> readers;
  private final CurrentUser currentUser;

  /**
   * Creates the resolver.
   *
   * @param ownerTypes owner entity types
   * @param readers permissions that may open the files (any of them)
   * @param currentUser current user
   */
  public PermissionFileAccess(
      Set<String> ownerTypes, List<String> readers, CurrentUser currentUser) {
    this.ownerTypes = Set.copyOf(ownerTypes);
    this.readers = List.copyOf(readers);
    this.currentUser = currentUser;
  }

  @Override
  public Set<String> ownerTypes() {
    return ownerTypes;
  }

  @Override
  public boolean mayRead(FileOwner owner, String documentType) {
    return readers.stream().anyMatch(currentUser::hasAuthority);
  }

  @Override
  public boolean mayStore(FileOwner owner, String documentType) {
    return false;
  }
}
