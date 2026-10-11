package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort;

/** Default {@link FileDropPort}: the in-system extract repository (OQ17). */
public class RepositoryFileDrop implements FileDropPort {

  private final ExtractRepositoryService repository;

  /**
   * Creates the adapter.
   *
   * @param repository extract repository
   */
  public RepositoryFileDrop(ExtractRepositoryService repository) {
    this.repository = repository;
  }

  @Override
  public DroppedFile drop(
      Long companyId, ExtractFile.Location location, DropContent file, ExtractFile.Origin origin) {
    return repository.store(companyId, location, file, origin);
  }
}
