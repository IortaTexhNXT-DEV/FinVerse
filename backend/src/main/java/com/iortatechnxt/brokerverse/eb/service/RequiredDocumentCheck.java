package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocumentRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The required-document check of a process (BRID-026; FR-EB-034): the authorised {@code
 * eb_required_document} rows of the process and the benefit lines concerned, against the document
 * types present. Used by the franchise request, the submission and the placement trigger.
 */
@Component
@Transactional(readOnly = true)
public class RequiredDocumentCheck {

  private final EbRequiredDocumentRepository required;
  private final LovService lovs;

  /**
   * Creates the check.
   *
   * @param required required documents
   * @param lovs document type and process labels
   */
  public RequiredDocumentCheck(EbRequiredDocumentRepository required, LovService lovs) {
    this.required = required;
    this.lovs = lovs;
  }

  /**
   * The requirements of a process.
   *
   * @param companyId company
   * @param process process type
   * @param lines benefit lines concerned
   * @return authorised requirements, mandatory first
   */
  public List<EbRequiredDocument> of(Long companyId, String process, Collection<String> lines) {
    return required.findByCompanyIdOrderByProcessTypeAscIdAsc(companyId).stream()
        .filter(r -> r.appliesTo(process, lines))
        .sorted((a, b) -> Boolean.compare(b.isMandatory(), a.isMandatory()))
        .toList();
  }

  /**
   * The mandatory document types missing.
   *
   * @param companyId company
   * @param process process type
   * @param lines benefit lines concerned
   * @param present document types present
   * @return missing types, in requirement order
   */
  public List<String> missing(
      Long companyId, String process, Collection<String> lines, Set<String> present) {
    return of(companyId, process, lines).stream()
        .filter(EbRequiredDocument::isMandatory)
        .map(EbRequiredDocument::getDocumentType)
        .filter(t -> !present.contains(t))
        .distinct()
        .toList();
  }

  /**
   * Refuses a process while a mandatory document is missing, naming the first one.
   *
   * @param companyId company
   * @param process process type
   * @param lines benefit lines concerned
   * @param present document types present
   */
  public void require(
      Long companyId, String process, Collection<String> lines, Set<String> present) {
    List<String> missing = missing(companyId, process, lines, present);
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "EB_DOCUMENT_MISSING",
          "Add the required document "
              + lovs.label("DOCUMENT_TYPE", missing.get(0))
              + " for "
              + lovs.label("EB_PROCESS_TYPE", process));
    }
  }
}
