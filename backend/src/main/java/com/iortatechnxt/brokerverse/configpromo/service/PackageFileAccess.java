package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Who may read the files of the configuration packages: the holders of a Configuration Promotion
 * permission. Package files are stored only by the module itself, never through the file API.
 */
@Component
public class PackageFileAccess implements FileOwnerAccess {

  private static final Set<String> READERS =
      Set.of(
          "CONFIG_EXPORT",
          "CONFIG_IMPORT_PREPARE",
          "CONFIG_IMPORT_APPROVE",
          "CONFIG_BASELINE_MANAGE");

  private final CurrentUser currentUser;

  /**
   * Creates the resolver.
   *
   * @param currentUser current user
   */
  public PackageFileAccess(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  public Set<String> ownerTypes() {
    return Set.of(PackageStore.OWNER_TYPE);
  }

  @Override
  public boolean mayRead(FileOwner owner, String documentType) {
    return READERS.stream().anyMatch(currentUser::hasAuthority);
  }

  @Override
  public boolean mayStore(FileOwner owner, String documentType) {
    return false;
  }
}
