package com.iortatechnxt.brokerverse.attachment.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentRepository;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import com.iortatechnxt.brokerverse.storage.service.LegacyFileTable;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Who may open the stored files of attachments (owner type {@value AttachmentService#OWNER_TYPE}):
 * the same checks as the attachment download, {@code ATTACHMENT_VIEW} and the document access
 * classes of the document ({@link DocumentAccessPolicy}); adding and removing files needs {@code
 * ATTACHMENT_MANAGE}. Also declares {@code doc_attachment_content} for the copy of the files kept
 * in the database before ST1.
 */
@Component
public class AttachmentFileAccess implements FileOwnerAccess {

  private final AttachmentRepository attachments;
  private final DocumentAccessPolicy policy;
  private final CurrentUser currentUser;

  /**
   * Creates the resolver.
   *
   * @param attachments attachment metadata
   * @param policy document access classes
   * @param currentUser current user
   */
  public AttachmentFileAccess(
      AttachmentRepository attachments, DocumentAccessPolicy policy, CurrentUser currentUser) {
    this.attachments = attachments;
    this.policy = policy;
    this.currentUser = currentUser;
  }

  @Override
  public Set<String> ownerTypes() {
    return Set.of(AttachmentService.OWNER_TYPE);
  }

  @Override
  public boolean mayRead(FileOwner owner, String documentType) {
    return currentUser.hasAuthority("ATTACHMENT_VIEW")
        && owner
            .numericId()
            .flatMap(attachments::findById)
            .filter(a -> !a.isDeleted())
            .map(policy::mayView)
            .orElse(false);
  }

  @Override
  public boolean mayStore(FileOwner owner, String documentType) {
    return currentUser.hasAuthority("ATTACHMENT_MANAGE");
  }

  /**
   * The attachment content kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable attachmentContentTable() {
    String from = " from doc_attachment_content c join doc_attachment a on a.id = c.attachment_id";
    return new LegacyFileTable(
        "doc_attachment_content",
        AttachmentService.OWNER_TYPE,
        FileOrigin.UPLOADED,
        "select c.attachment_id as file_key, cast(null as bigint) as company_id,"
            + " cast(a.id as varchar) as owner_id, "
            + AttachmentRecordClasses.sqlCase("a.document_type")
            + " as record_class, a.document_type, a.file_name, a.content_type,"
            + " a.sha256 as recorded_sha256, c.content"
            + from
            + " where a.stored_file_id is null and c.attachment_id > ?"
            + " order by c.attachment_id limit ?",
        "update doc_attachment set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(a.stored_file_id) as moved" + from);
  }
}
