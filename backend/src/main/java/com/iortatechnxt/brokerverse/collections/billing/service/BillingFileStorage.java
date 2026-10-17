package com.iortatechnxt.brokerverse.collections.billing.service;

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
 * The stored PDFs of statements of account (owner type {@value BillingStatementService#ENTITY}):
 * opened by holders of {@code CLX_VIEW}, as for the statement screen; rendered by the application
 * only. Also declares {@code clx_billing_document} for the copy of the PDFs kept in the database
 * before ST1.
 */
@Configuration(proxyBeanMethods = false)
public class BillingFileStorage {

  /**
   * Who may open the files.
   *
   * @param currentUser current user
   * @return resolver
   */
  @Bean
  FileOwnerAccess billingFileAccess(CurrentUser currentUser) {
    return new PermissionFileAccess(
        Set.of(BillingStatementService.ENTITY), List.of("CLX_VIEW"), currentUser);
  }

  /**
   * Statement PDFs kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable billingDocumentTable() {
    return new LegacyFileTable(
        "clx_billing_document",
        BillingStatementService.ENTITY,
        FileOrigin.GENERATED,
        "select d.id as file_key, s.company_id, cast(d.statement_id as varchar) as owner_id, '"
            + BillingStatementService.RECORD_CLASS
            + "' as record_class, cast(null as varchar) as document_type, d.file_name,"
            + " d.content_type, cast(null as varchar) as recorded_sha256, d.content"
            + " from clx_billing_document d join clx_billing_statement s on s.id = d.statement_id"
            + " where d.content is not null and d.stored_file_id is null and d.id > ?"
            + " order by d.id limit ?",
        "update clx_billing_document set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from clx_billing_document where content is not null");
  }
}
