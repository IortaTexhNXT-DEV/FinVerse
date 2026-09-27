package com.iortatechnxt.brokerverse.cashiering.service;

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
 * The stored merged PDFs of receipt print batches (owner type {@value
 * BatchPrintService#OWNER_TYPE}): opened by holders of {@code CASH_PRINT}, as for the batch screen;
 * stored by the application only. Also declares {@code csh_print_batch} for the copy of the batches
 * kept in the database before ST1.
 */
@Configuration(proxyBeanMethods = false)
public class PrintBatchFileStorage {

  /**
   * Who may open the files.
   *
   * @param currentUser current user
   * @return resolver
   */
  @Bean
  FileOwnerAccess printBatchFileAccess(CurrentUser currentUser) {
    return new PermissionFileAccess(
        Set.of(BatchPrintService.OWNER_TYPE), List.of("CASH_PRINT"), currentUser);
  }

  /**
   * Merged PDFs of print batches kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable printBatchTable() {
    return new LegacyFileTable(
        "csh_print_batch",
        BatchPrintService.OWNER_TYPE,
        FileOrigin.GENERATED,
        "select b.id as file_key, b.company_id, cast(b.id as varchar) as owner_id, '"
            + BatchPrintService.RECORD_CLASS
            + "' as record_class, cast(null as varchar) as document_type,"
            + " coalesce(b.file_name, b.batch_no || '.pdf') as file_name,"
            + " 'application/pdf' as content_type, cast(null as varchar) as recorded_sha256,"
            + " b.content from csh_print_batch b where b.content is not null"
            + " and b.stored_file_id is null and b.id > ? order by b.id limit ?",
        "update csh_print_batch set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from csh_print_batch where content is not null");
  }
}
