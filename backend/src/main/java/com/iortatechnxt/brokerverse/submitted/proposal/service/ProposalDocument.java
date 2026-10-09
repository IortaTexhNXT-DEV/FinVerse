package com.iortatechnxt.brokerverse.submitted.proposal.service;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposalBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposalBatchRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The renewal proposal of a record as PDF (FR-SP-066): the assured, the expiring policy, the
 * insurer proposed, the rate applied and the premium, rendered when downloaded. The layout follows
 * the document template of the proposal once BDOI provides it.
 */
@Component
@Transactional(readOnly = true)
public class ProposalDocument {

  private static final String PDF = "application/pdf";

  private final SbmProposalService proposals;
  private final SbmProposalBatchRepository batches;
  private final DocumentComposer composer;
  private final OrganizationDirectory organization;

  /**
   * Creates the renderer.
   *
   * @param proposals proposals
   * @param batches batches
   * @param composer PDF rendering
   * @param organization company master (letterhead)
   */
  public ProposalDocument(
      SbmProposalService proposals,
      SbmProposalBatchRepository batches,
      DocumentComposer composer,
      OrganizationDirectory organization) {
    this.proposals = proposals;
    this.batches = batches;
    this.composer = composer;
    this.organization = organization;
  }

  /**
   * The PDF of a proposal.
   *
   * @param id proposal
   * @return download
   */
  public FileDownload pdf(Long id) {
    SbmProposal proposal = proposals.proposal(id);
    SbmPolicy p = proposals.policy(proposal.getCompanyId(), proposal.getPolicyId());
    String batchNo =
        batches.findById(proposal.getBatchId()).map(SbmProposalBatch::getBatchNo).orElse("");
    String reference = batchNo + " " + p.getSbmNo() + " v" + proposal.getVersionNo();
    List<Field> facts =
        List.of(
            new Field("Assured", p.getAssured().assuredName()),
            new Field("Expiring policy", p.getTerms().policyNo()),
            new Field("Expiry", DisplayFormat.date(p.getTerms().expiryDate())),
            new Field("Proposed insurer", proposal.getInsurerCode()),
            new Field("Sum insured", amount(proposal.getSumInsured())),
            new Field("Rate", proposal.getAppliedRate().stripTrailingZeros().toPlainString() + "%"),
            new Field("Premium", amount(proposal.getPremium())));
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                organization.company(proposal.getCompanyId()).name(),
                "Renewal Proposal",
                reference,
                List.of(new Fields("Proposal", facts)),
                List.of("Prepared by", "Approved by"),
                null));
    return FileDownload.inline(reference.replace(' ', '_') + ".pdf", PDF, pdf);
  }

  private static String amount(BigDecimal value) {
    return value == null ? "" : DisplayFormat.amount(value);
  }
}
