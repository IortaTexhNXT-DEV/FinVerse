package com.iortatechnxt.brokerverse.placement.service;

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
 * The stored files of placement slips (owner type {@value PlacementSlipService#OWNER_TYPE}): opened
 * by the readers of the placement screens ({@code ACCOUNT_VIEW}, {@code PLACEMENT_MANAGE}, {@code
 * BILLING_MANAGE}); rendered by the application only. Also declares {@code plc_slip_file} for the
 * copy of the files kept in the database before ST1.
 */
@Configuration(proxyBeanMethods = false)
public class SlipFileStorage {

  /**
   * Who may open the files.
   *
   * @param currentUser current user
   * @return resolver
   */
  @Bean
  FileOwnerAccess slipFileAccess(CurrentUser currentUser) {
    return new PermissionFileAccess(
        Set.of(PlacementSlipService.OWNER_TYPE),
        List.of("ACCOUNT_VIEW", "PLACEMENT_MANAGE", "BILLING_MANAGE"),
        currentUser);
  }

  /**
   * Slip files kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable slipFileTable() {
    return new LegacyFileTable(
        "plc_slip_file",
        PlacementSlipService.OWNER_TYPE,
        FileOrigin.GENERATED,
        "select f.id as file_key, s.company_id, cast(f.slip_id as varchar) as owner_id, '"
            + PlacementSlipService.RECORD_CLASS
            + "' as record_class, f.format as document_type, f.file_name,"
            + " case f.format when 'PDF' then 'application/pdf' else"
            + " 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' end"
            + " as content_type, f.sha256 as recorded_sha256, f.content"
            + " from plc_slip_file f join plc_slip s on s.id = f.slip_id"
            + " where f.content is not null and f.stored_file_id is null and f.id > ?"
            + " order by f.id limit ?",
        "update plc_slip_file set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from plc_slip_file where content is not null");
  }
}
