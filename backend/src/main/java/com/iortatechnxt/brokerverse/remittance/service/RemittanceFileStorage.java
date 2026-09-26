package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import com.iortatechnxt.brokerverse.storage.service.LegacyFileTable;
import com.iortatechnxt.brokerverse.storage.service.PermissionFileAccess;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The stored documents of remittance batches (owner type {@value BatchDocumentStore#OWNER_TYPE}):
 * opened by the remittance team and approvers, as for the batch screen; stored by the application
 * only. Also declares {@code rem_batch_document} for the copy of the documents kept in the database
 * before ST1.
 */
@Configuration(proxyBeanMethods = false)
public class RemittanceFileStorage {

  /**
   * Who may open the files.
   *
   * @param currentUser current user
   * @return resolver
   */
  @Bean
  FileOwnerAccess remittanceFileAccess(CurrentUser currentUser) {
    return new PermissionFileAccess(
        Set.of(BatchDocumentStore.OWNER_TYPE),
        List.of("REMIT_PROCESS", "REMIT_EXTRACT", "REMIT_APPROVE", "REMIT_OR_UPLOAD"),
        currentUser);
  }

  /**
   * Batch documents kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable remittanceDocumentTable() {
    return new LegacyFileTable(
        "rem_batch_document",
        BatchDocumentStore.OWNER_TYPE,
        FileOrigin.GENERATED,
        "select d.id as file_key, b.company_id, cast(d.id as varchar) as owner_id, '"
            + BatchDocumentStore.RECORD_CLASS
            + "' as record_class, d.kind as document_type, d.file_name, d.content_type,"
            + " cast(null as varchar) as recorded_sha256, d.content"
            + " from rem_batch_document d join rem_batch b on b.id = d.batch_id"
            + " where d.content is not null and d.stored_file_id is null and d.id > ?"
            + " order by d.id limit ?",
        "update rem_batch_document set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from rem_batch_document where content is not null");
  }
}
