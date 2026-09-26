package com.iortatechnxt.brokerverse.messaging.service;

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
 * The stored attachments of e-mails (owner type {@value MessageService#OWNER_TYPE}): opened by
 * holders of {@code MESSAGE_VIEW}, as for the outbox; stored by the application only. Also declares
 * {@code msg_outbound_attachment} for the copy of the attachments kept in the database before ST1.
 */
@Configuration(proxyBeanMethods = false)
public class MessageFileStorage {

  /**
   * Who may open the files.
   *
   * @param currentUser current user
   * @return resolver
   */
  @Bean
  FileOwnerAccess messageFileAccess(CurrentUser currentUser) {
    return new PermissionFileAccess(
        Set.of(MessageService.OWNER_TYPE), List.of("MESSAGE_VIEW"), currentUser);
  }

  /**
   * E-mail attachments kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable messageAttachmentTable() {
    return new LegacyFileTable(
        "msg_outbound_attachment",
        MessageService.OWNER_TYPE,
        FileOrigin.GENERATED,
        "select a.id as file_key, m.company_id, cast(a.message_id as varchar) as owner_id, '"
            + MessageService.RECORD_CLASS
            + "' as record_class, m.purpose as document_type, a.file_name,"
            + " a.mime_type as content_type, a.sha256 as recorded_sha256, a.content"
            + " from msg_outbound_attachment a join msg_outbound m on m.id = a.message_id"
            + " where a.content is not null and a.stored_file_id is null and a.id > ?"
            + " order by a.id limit ?",
        "update msg_outbound_attachment set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from msg_outbound_attachment where content is not null");
  }
}
