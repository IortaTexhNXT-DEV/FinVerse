package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.DocumentRequirement;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.DocumentRequirementRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The standard claim document checklist (FRS BRD-07 FRCLM.006.01 and 006.02, OOTB point G-CLM-01):
 * the documents required per claim type, maintained on Claims Setup; a claim moves to Newly Filed
 * Claim - with complete documents only when every required document of its claim type is uploaded
 * on the claim.
 */
@Service
@Transactional
public class DocumentChecklist {

  /** Document types of the files uploaded on a claim, directly or linked. */
  static final String UPLOADED =
      "select a.document_type from doc_attachment a where a.entity_type = ? and a.entity_id = ?"
          + " and a.document_type is not null"
          + " union select a.document_type from doc_attachment_link l"
          + " join doc_attachment a on a.id = l.attachment_id"
          + " where l.entity_type = ? and l.entity_id = ? and a.document_type is not null";

  private static final String ENTITY = "BrokerClaimDocumentRequirement";
  private static final String DOCUMENT_TYPE = "DOCUMENT_TYPE";

  private final DocumentRequirementRepository requirements;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the checklist.
   *
   * @param requirements requirements
   * @param lovs claim types and document types
   * @param audit audit trail
   * @param jdbc uploaded documents
   * @param clock clock
   */
  public DocumentChecklist(
      DocumentRequirementRepository requirements,
      LovService lovs,
      AuditTrailService audit,
      JdbcTemplate jdbc,
      Clock clock) {
    this.requirements = requirements;
    this.lovs = lovs;
    this.audit = audit;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * Every requirement of the checklist.
   *
   * @return requirements
   */
  @Transactional(readOnly = true)
  public List<DocumentRequirement> requirements() {
    return requirements.findAllByOrderByClaimTypeAscDocumentTypeAsc();
  }

  /**
   * Adds or changes a requirement (Claims Unit Head).
   *
   * @param claimType claim type ({@code BCL_CLAIM_TYPE})
   * @param documentType document type ({@code DOCUMENT_TYPE})
   * @param required whether the document must be uploaded
   * @param active whether the requirement applies
   * @return the requirement
   */
  public DocumentRequirement save(
      String claimType, String documentType, boolean required, boolean active) {
    var today = BusinessClock.today(clock);
    lovs.requireValid(ClaimCodes.LOV_CLAIM_TYPE, claimType, today);
    lovs.requireValid(DOCUMENT_TYPE, documentType, today);
    DocumentRequirement r =
        requirements
            .findByClaimTypeAndDocumentType(claimType, documentType)
            .orElseGet(
                () ->
                    requirements.save(new DocumentRequirement(claimType, documentType, required)));
    r.update(required, active);
    audit.record(
        ENTITY,
        claimType + "/" + documentType,
        AuditAction.UPDATE,
        (required ? "Required" : "Optional") + (active ? "" : ", inactive"));
    return r;
  }

  /**
   * The checklist of a claim with what is uploaded.
   *
   * @param claim claim
   * @return items of its claim type
   */
  @Transactional(readOnly = true)
  public List<Item> of(Claim claim) {
    Set<String> uploaded = uploaded(claim);
    return requirements
        .findByClaimTypeAndActiveTrueOrderByDocumentTypeAsc(claim.getLoss().getClaimType())
        .stream()
        .map(
            r ->
                new Item(
                    r.getDocumentType(),
                    lovs.label(DOCUMENT_TYPE, r.getDocumentType()),
                    r.isRequired(),
                    uploaded.contains(r.getDocumentType())))
        .toList();
  }

  /**
   * Refuses a claim with a required document missing.
   *
   * @param claim claim
   */
  public void requireComplete(Claim claim) {
    List<String> missing =
        of(claim).stream().filter(i -> i.required() && !i.uploaded()).map(Item::label).toList();
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "BCL_DOCUMENTS_MISSING",
          "Upload the required documents first: " + String.join(", ", missing));
    }
  }

  private Set<String> uploaded(Claim claim) {
    String id = claim.getId().toString();
    return new HashSet<>(
        jdbc.queryForList(
            UPLOADED, String.class, ClaimCodes.ENTITY_TYPE, id, ClaimCodes.ENTITY_TYPE, id));
  }

  /**
   * A document of the checklist of a claim.
   *
   * @param documentType document type
   * @param label label
   * @param required whether required
   * @param uploaded whether a file of the type is on the claim
   */
  public record Item(String documentType, String label, boolean required, boolean uploaded) {}
}
