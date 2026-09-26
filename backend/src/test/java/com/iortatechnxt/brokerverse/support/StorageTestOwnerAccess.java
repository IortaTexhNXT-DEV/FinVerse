package com.iortatechnxt.brokerverse.support;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Owner permission resolver of the storage tests: records of type {@value #TYPE} are open to every
 * signed-in user except those whose key starts with {@value #LOCKED_PREFIX}.
 */
@Component
public class StorageTestOwnerAccess implements FileOwnerAccess {

  public static final String TYPE = "StorageTestRecord";
  public static final String LOCKED_PREFIX = "LOCKED-";

  private final CurrentUser currentUser;

  StorageTestOwnerAccess(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  public Set<String> ownerTypes() {
    return Set.of(TYPE);
  }

  @Override
  public boolean mayRead(FileOwner owner, String documentType) {
    return allowed(owner);
  }

  @Override
  public boolean mayStore(FileOwner owner, String documentType) {
    return allowed(owner);
  }

  private boolean allowed(FileOwner owner) {
    return currentUser.optionalUsername().isPresent()
        && !owner.entityId().startsWith(LOCKED_PREFIX);
  }
}
