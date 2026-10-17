package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFileRepository;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import com.iortatechnxt.brokerverse.storage.service.LegacyFileTable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Who may open the stored files of the extract repository (owner type {@value
 * ExtractRepositoryService#OWNER_TYPE}): {@code OPS_VIEW} (Interfaces screen), and the users of the
 * module that produced the file where that module offers its own download (DP billing files of
 * commission, production registers of product reconciliation). Files are stored by the application
 * only. Also declares {@code ops_extract_file} for the copy of the files kept in the database
 * before ST1.
 */
@Component
public class ExtractFileAccess implements FileOwnerAccess {

  private static final String OPS_VIEW = "OPS_VIEW";

  /** Permissions of the modules with their own download of their files. */
  private static final Map<String, List<String>> BY_MODULE =
      Map.of(
          "COMMISSION", List.of("COMMREC_PROCESS", "COMMREC_APPROVE"),
          "PRODRECON", List.of("RECON_PROCESS", "RECON_SEND"));

  private final ExtractFileRepository files;
  private final CurrentUser currentUser;

  /**
   * Creates the resolver.
   *
   * @param files extract files
   * @param currentUser current user
   */
  public ExtractFileAccess(ExtractFileRepository files, CurrentUser currentUser) {
    this.files = files;
    this.currentUser = currentUser;
  }

  @Override
  public Set<String> ownerTypes() {
    return Set.of(ExtractRepositoryService.OWNER_TYPE);
  }

  @Override
  public boolean mayRead(FileOwner owner, String documentType) {
    return currentUser.hasAuthority(OPS_VIEW)
        || owner
            .numericId()
            .flatMap(files::findById)
            .map(ExtractFile::getSourceModule)
            .map(
                m ->
                    BY_MODULE.getOrDefault(m, List.of()).stream()
                        .anyMatch(currentUser::hasAuthority))
            .orElse(false);
  }

  @Override
  public boolean mayStore(FileOwner owner, String documentType) {
    return false;
  }

  /**
   * Extract files kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable extractFileTable() {
    return new LegacyFileTable(
        "ops_extract_file",
        ExtractRepositoryService.OWNER_TYPE,
        FileOrigin.GENERATED,
        "select f.id as file_key, f.company_id, cast(f.id as varchar) as owner_id, '"
            + ExtractRepositoryService.RECORD_CLASS
            + "' as record_class, f.source_module as document_type, f.file_name,"
            + " f.content_type, f.sha256 as recorded_sha256, f.content"
            + " from ops_extract_file f where f.content is not null and f.stored_file_id is null"
            + " and f.id > ? order by f.id limit ?",
        "update ops_extract_file set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from ops_extract_file where content is not null");
  }
}
