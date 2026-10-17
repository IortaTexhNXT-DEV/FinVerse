package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Routes the permission check of a file to the {@link FileOwnerAccess} of its owner type. Owner
 * types without a resolver are refused.
 */
@Component
public class FileAccessPolicy {

  private final Map<String, FileOwnerAccess> byType = new HashMap<>();

  /**
   * Collects the resolvers.
   *
   * @param resolvers resolvers of the modules
   */
  public FileAccessPolicy(List<FileOwnerAccess> resolvers) {
    for (FileOwnerAccess resolver : resolvers) {
      for (String type : resolver.ownerTypes()) {
        FileOwnerAccess previous = byType.putIfAbsent(type, resolver);
        if (previous != null) {
          throw new IllegalStateException("Two file access resolvers for owner type " + type);
        }
      }
    }
  }

  /**
   * Whether the current user may read a file of a record.
   *
   * @param owner owning record
   * @param documentType document type
   * @return true when the owner's resolver allows it
   */
  public boolean mayRead(FileOwner owner, String documentType) {
    return resolver(owner).map(r -> r.mayRead(owner, documentType)).orElse(false);
  }

  /**
   * Whether the current user may add or remove a file of a record.
   *
   * @param owner owning record
   * @param documentType document type
   * @return true when the owner's resolver allows it
   */
  public boolean mayStore(FileOwner owner, String documentType) {
    return resolver(owner).map(r -> r.mayStore(owner, documentType)).orElse(false);
  }

  /**
   * Refuses (HTTP 403) a user who may not read the files of a record.
   *
   * @param owner owning record
   * @param documentType document type
   */
  public void requireRead(FileOwner owner, String documentType) {
    if (!mayRead(owner, documentType)) {
      throw new AccessDeniedException("You may not open the files of this record");
    }
  }

  /**
   * Refuses (HTTP 403) a user who may not add or remove files of a record.
   *
   * @param owner owning record
   * @param documentType document type
   */
  public void requireStore(FileOwner owner, String documentType) {
    if (!mayStore(owner, documentType)) {
      throw new AccessDeniedException("You may not change the files of this record");
    }
  }

  private Optional<FileOwnerAccess> resolver(FileOwner owner) {
    return Optional.ofNullable(byType.get(owner.entityType()));
  }
}
