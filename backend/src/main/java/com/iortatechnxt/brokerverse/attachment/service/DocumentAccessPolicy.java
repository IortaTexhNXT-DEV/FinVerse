package com.iortatechnxt.brokerverse.attachment.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.DocumentAccess;
import com.iortatechnxt.brokerverse.attachment.domain.DocumentAccessRepository;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Document access classes (BRID-025; cross-BRD work item P3; table {@code att_document_access},
 * V1031): a document whose type has access rows is listed and downloaded only by holders of one of
 * the rows' permissions (Marketing, Processing, Collection, ...). A type without rows, and a
 * document without a type, keep today's behaviour (every {@code ATTACHMENT_VIEW} holder).
 * Background processing (no authenticated user) is not restricted.
 */
@Component
@Transactional(readOnly = true)
public class DocumentAccessPolicy {

  private static final String ENTITY = "Attachment";

  private final DocumentAccessRepository rows;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the policy.
   *
   * @param rows access class rows
   * @param audit audit trail (refused downloads)
   * @param currentUser current user
   */
  public DocumentAccessPolicy(
      DocumentAccessRepository rows, AuditTrailService audit, CurrentUser currentUser) {
    this.rows = rows;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /**
   * The documents of a list the current user may see.
   *
   * @param documents documents
   * @return the visible ones, in the same order
   */
  public List<Attachment> visible(List<Attachment> documents) {
    if (unrestricted()) {
      return documents;
    }
    Map<String, Set<String>> allowed = allowedByType(typesOf(documents));
    return documents.stream().filter(d -> mayView(d, allowed)).toList();
  }

  /**
   * Whether the current user may see a document.
   *
   * @param document document
   * @return true when allowed
   */
  public boolean mayView(Attachment document) {
    return unrestricted() || mayView(document, allowedByType(typesOf(List.of(document))));
  }

  /**
   * Refuses a document the current user may not see (HTTP 403); the refusal is audited in its own
   * transaction, so it is kept although the request fails.
   *
   * @param document document
   */
  public void requireView(Attachment document) {
    if (!mayView(document)) {
      audit.recordIndependently(
          currentUser.username(),
          ENTITY,
          document.getId(),
          AuditAction.REJECT,
          "Access refused to "
              + document.getFileName()
              + ": document type "
              + document.getDocumentType()
              + " is restricted by its access class");
      throw new AccessDeniedException(
          "You are not permitted to open documents of type " + document.getDocumentType());
    }
  }

  /**
   * Every access class row, for the set-up screen and the tests.
   *
   * @return rows by document type
   */
  public List<DocumentAccess> rows() {
    return rows.findAllByOrderByDocumentTypeAscAccessClassAscPermissionAsc();
  }

  private boolean mayView(Attachment document, Map<String, Set<String>> allowed) {
    if (document.getDocumentType() == null) {
      return true;
    }
    Set<String> permissions = allowed.get(document.getDocumentType());
    return permissions == null || permissions.stream().anyMatch(currentUser::hasAuthority);
  }

  private boolean unrestricted() {
    return currentUser.optionalUsername().isEmpty();
  }

  private Map<String, Set<String>> allowedByType(Collection<String> types) {
    if (types.isEmpty()) {
      return Map.of();
    }
    return rows.findByDocumentTypeIn(types).stream()
        .collect(
            Collectors.groupingBy(
                DocumentAccess::getDocumentType,
                Collectors.mapping(DocumentAccess::getPermission, Collectors.toSet())));
  }

  private static Set<String> typesOf(List<Attachment> documents) {
    return documents.stream()
        .map(Attachment::getDocumentType)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
